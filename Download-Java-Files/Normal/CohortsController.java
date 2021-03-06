package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.CdrQuery;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortAnnotationsRequest;
import org.pmiops.workbench.model.CohortAnnotationsResponse;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DataTableSpecification;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortsController implements CohortsApiDelegate {

  @VisibleForTesting
  static final int MAX_PAGE_SIZE = 10000;
  @VisibleForTesting
  static final int DEFAULT_PAGE_SIZE = 1000;
  private static final Logger log = Logger.getLogger(CohortsController.class.getName());

  /**
   * Converter function from backend representation (used with Hibernate) to client representation
   * (generated by Swagger).
   */
  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          Cohort result = new Cohort()
              .etag(Etags.fromVersion(cohort.getVersion()))
              .lastModifiedTime(cohort.getLastModifiedTime().getTime())
              .creationTime(cohort.getCreationTime().getTime())
              .criteria(cohort.getCriteria())
              .description(cohort.getDescription())
              .id(cohort.getCohortId())
              .name(cohort.getName())
              .type(cohort.getType());
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
          return result;
        }
      };

  private final WorkspaceService workspaceService;
  private final CohortDao cohortDao;
  private final CdrVersionDao cdrVersionDao;
  private final CohortFactory cohortFactory;
  private final CohortReviewDao cohortReviewDao;
  private final ConceptSetDao conceptSetDao;
  private final CohortMaterializationService cohortMaterializationService;
  private Provider<User> userProvider;
  private final Clock clock;
  private final CdrVersionService cdrVersionService;
  private final UserRecentResourceService userRecentResourceService;

  @Autowired
  CohortsController(
      WorkspaceService workspaceService,
      CohortDao cohortDao,
      CdrVersionDao cdrVersionDao,
      CohortFactory cohortFactory,
      CohortReviewDao cohortReviewDao,
      ConceptSetDao conceptSetDao,
      CohortMaterializationService cohortMaterializationService,
      Provider<User> userProvider,
      Clock clock,
      CdrVersionService cdrVersionService,
      UserRecentResourceService userRecentResourceService) {
    this.workspaceService = workspaceService;
    this.cohortDao = cohortDao;
    this.cdrVersionDao = cdrVersionDao;
    this.cohortFactory = cohortFactory;
    this.cohortReviewDao = cohortReviewDao;
    this.conceptSetDao = conceptSetDao;
    this.cohortMaterializationService = cohortMaterializationService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.cdrVersionService = cdrVersionService;
    this.userRecentResourceService = userRecentResourceService;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  private void checkForDuplicateCohortNameException(String newCohortName, Workspace workspace) {
    if (cohortDao.findCohortByNameAndWorkspaceId(newCohortName, workspace.getWorkspaceId())
        != null) {
      throw new BadRequestException(String.format("Cohort \"/%s/%s/%s\" already exists.",
          workspace.getWorkspaceNamespace(), workspace.getWorkspaceId(), newCohortName));
    }
  }

  @Override
  public ResponseEntity<Cohort> createCohort(String workspaceNamespace, String workspaceId,
      Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    checkForDuplicateCohortNameException(cohort.getName(), workspace);

    org.pmiops.workbench.db.model.Cohort newCohort = cohortFactory
        .createCohort(cohort, userProvider.get(), workspace.getWorkspaceId());
    try {
      // TODO Make this a pre-check within a transaction?
      newCohort = cohortDao.save(newCohort);
      userRecentResourceService.updateCohortEntry(workspace.getWorkspaceId(),
          userProvider.get().getUserId(),
          newCohort.getCohortId(),
          newCohort.getLastModifiedTime());
    } catch (DataIntegrityViolationException e) {
      // TODO The exception message doesn't show up anywhere; neither logged nor returned to the
      // client by Spring (the client gets a default reason string).
      throw new ServerErrorException(
          String.format("Could not save Cohort (\"/%s/%s/%s\")", workspace.getWorkspaceNamespace(),
              workspace.getWorkspaceId(), newCohort.getName()),
          e
      );
    }

    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(newCohort));
  }

  @Override
  public ResponseEntity<Cohort> duplicateCohort(String workspaceNamespace, String workspaceId,
      DuplicateCohortRequest params) {
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    checkForDuplicateCohortNameException(params.getNewName(), workspace);

    org.pmiops.workbench.db.model.Cohort originalCohort = getDbCohort(workspaceNamespace,
        workspaceId, params.getOriginalCohortId());
    org.pmiops.workbench.db.model.Cohort newCohort = cohortFactory
        .duplicateCohort(params.getNewName(), userProvider.get(), workspace, originalCohort);
    try {
      newCohort = cohortDao.save(newCohort);
      userRecentResourceService.updateCohortEntry(workspace.getWorkspaceId(),
          userProvider.get().getUserId(),
          newCohort.getCohortId(),
          new Timestamp(clock.instant().toEpochMilli()));
    } catch (Exception e) {
      throw new ServerErrorException(
          String.format("Could not save Cohort (\"/%s/%s/%s\")", workspace.getWorkspaceNamespace(),
              workspace.getWorkspaceId(), newCohort.getName()),
          e
      );
    }

    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(newCohort));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohort(String workspaceNamespace, String workspaceId,
      Long cohortId) {
    // This also enforces registered auth domain.
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    cohortDao.delete(dbCohort);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Cohort> getCohort(String workspaceNamespace, String workspaceId,
      Long cohortId) {
    // This also enforces registered auth domain.
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    // This also enforces registered auth domain.
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    Workspace workspace = workspaceService.getRequiredWithCohorts(workspaceNamespace, workspaceId);
    CohortListResponse response = new CohortListResponse();
    Set<org.pmiops.workbench.db.model.Cohort> cohorts = workspace.getCohorts();
    if (cohorts != null) {
      response.setItems(cohorts.stream()
          .map(TO_CLIENT_COHORT)
          .sorted(Comparator.comparing(c -> c.getName()))
          .collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(String workspaceNamespace, String workspaceId,
      Long cohortId, Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceService
        .enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.db.model.Cohort dbCohort = getDbCohort(workspaceNamespace, workspaceId,
        cohortId);
    if (Strings.isNullOrEmpty(cohort.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(cohort.getEtag());
    if (dbCohort.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated cohort version");
    }
    if (cohort.getType() != null) {
      dbCohort.setType(cohort.getType());
    }
    if (cohort.getName() != null) {
      dbCohort.setName(cohort.getName());
    }
    if (cohort.getDescription() != null) {
      dbCohort.setDescription(cohort.getDescription());
    }
    if (cohort.getCriteria() != null) {
      dbCohort.setCriteria(cohort.getCriteria());
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbCohort.setLastModifiedTime(now);
    try {
      // The version asserted on save is the same as the one we read via
      // getRequired() above, see RW-215 for details.
      dbCohort = cohortDao.save(dbCohort);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort update", e);
      throw new ConflictException("Failed due to concurrent cohort modification");
    }
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  private Set<Long> getConceptIds(Workspace workspace, TableQuery tableQuery) {
    String conceptSetName = tableQuery.getConceptSetName();
    if (conceptSetName != null) {
      ConceptSet conceptSet = conceptSetDao.findConceptSetByNameAndWorkspaceId(conceptSetName,
          workspace.getWorkspaceId());
      if (conceptSet == null) {
        throw new NotFoundException(
            String.format("Couldn't find concept set with name %s in workspace %s/%s",
                conceptSetName, workspace.getWorkspaceNamespace(), workspace.getWorkspaceId()));
      }
      String tableName = ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(conceptSet.getDomainEnum());
      if (tableName == null) {
        throw new ServerErrorException("Couldn't find table for domain: " +
            conceptSet.getDomainEnum());
      }
      if (!tableName.equals(tableQuery.getTableName())) {
        throw new BadRequestException(
            String.format("Can't use concept set for domain %s with table %s",
                conceptSet.getDomainEnum(),
                tableQuery.getTableName()));
      }
      return conceptSet.getConceptIds();
    }
    return null;
  }

  @Override
  public ResponseEntity<MaterializeCohortResponse> materializeCohort(String workspaceNamespace,
      String workspaceId, MaterializeCohortRequest request) {
    // This also enforces registered auth domain.
    Workspace workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CdrVersion cdrVersion = workspace.getCdrVersion();

    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(String.format("Couldn't find CDR version with name %s",
            request.getCdrVersionName()));
      }
      cdrVersionService.setCdrVersion(cdrVersion);
    }
    String cohortSpec;
    CohortReview cohortReview = null;
    if (request.getCohortName() != null) {
      org.pmiops.workbench.db.model.Cohort cohort =
          cohortDao
              .findCohortByNameAndWorkspaceId(request.getCohortName(), workspace.getWorkspaceId());
      if (cohort == null) {
        throw new NotFoundException(
            String.format("Couldn't find cohort with name %s in workspace %s/%s",
                request.getCohortName(), workspaceNamespace, workspaceId));
      }
      cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohort.getCohortId(),
          cdrVersion.getCdrVersionId());
      cohortSpec = cohort.getCriteria();
    } else if (request.getCohortSpec() != null) {
      cohortSpec = request.getCohortSpec();
      if (request.getStatusFilter() != null) {
        throw new BadRequestException("statusFilter cannot be used with cohortSpec");
      }
    } else {
      throw new BadRequestException("Must specify either cohortName or cohortSpec");
    }
    Set<Long> conceptIds = null;
    if (request.getFieldSet() != null && request.getFieldSet().getTableQuery() != null) {
      conceptIds = getConceptIds(workspace, request.getFieldSet().getTableQuery());
    }

    Integer pageSize = request.getPageSize();
    if (pageSize == null || pageSize == 0) {
      request.setPageSize(DEFAULT_PAGE_SIZE);
    } else if (pageSize < 0) {
      throw new BadRequestException(
          String.format("Invalid page size: %s; must be between 1 and %d", pageSize,
              MAX_PAGE_SIZE));
    } else if (pageSize > MAX_PAGE_SIZE) {
      request.setPageSize(MAX_PAGE_SIZE);
    }

    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(
        cohortReview, cohortSpec, conceptIds, request);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CdrQuery> getDataTableQuery(String workspaceNamespace, String workspaceId,
      DataTableSpecification request) {
    Workspace workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CdrVersion cdrVersion = workspace.getCdrVersion();

    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(String.format("Couldn't find CDR version with name %s",
            request.getCdrVersionName()));
      }
      cdrVersionService.setCdrVersion(cdrVersion);
    }
    String cohortSpec;
    CohortReview cohortReview = null;
    if (request.getCohortName() != null) {
      org.pmiops.workbench.db.model.Cohort cohort =
          cohortDao
              .findCohortByNameAndWorkspaceId(request.getCohortName(), workspace.getWorkspaceId());
      if (cohort == null) {
        throw new NotFoundException(
            String.format("Couldn't find cohort with name %s in workspace %s/%s",
                request.getCohortName(), workspaceNamespace, workspaceId));
      }
      cohortReview = cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohort.getCohortId(),
          cdrVersion.getCdrVersionId());
      cohortSpec = cohort.getCriteria();
    } else if (request.getCohortSpec() != null) {
      cohortSpec = request.getCohortSpec();
      if (request.getStatusFilter() != null) {
        throw new BadRequestException("statusFilter cannot be used with cohortSpec");
      }
    } else {
      throw new BadRequestException("Must specify either cohortName or cohortSpec");
    }
    Set<Long> conceptIds = getConceptIds(workspace, request.getTableQuery());
    CdrQuery query = cohortMaterializationService
        .getCdrQuery(cohortSpec, request, cohortReview, conceptIds);
    return ResponseEntity.ok(query);
  }

  @Override
  public ResponseEntity<CohortAnnotationsResponse> getCohortAnnotations(String workspaceNamespace,
      String workspaceId, CohortAnnotationsRequest request) {
    Workspace workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    CdrVersion cdrVersion = workspace.getCdrVersion();
    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(String.format("Couldn't find CDR version with name %s",
            request.getCdrVersionName()));
      }
    }
    org.pmiops.workbench.db.model.Cohort cohort =
        cohortDao
            .findCohortByNameAndWorkspaceId(request.getCohortName(), workspace.getWorkspaceId());
    if (cohort == null) {
      throw new NotFoundException(
          String.format("Couldn't find cohort with name %s in workspace %s/%s",
              request.getCohortName(), workspaceNamespace, workspaceId));
    }
    CohortReview cohortReview = cohortReviewDao
        .findCohortReviewByCohortIdAndCdrVersionId(cohort.getCohortId(),
            cdrVersion.getCdrVersionId());
    if (cohortReview == null) {
      return ResponseEntity
          .ok(new CohortAnnotationsResponse().columns(request.getAnnotationQuery().getColumns()));
    }
    return ResponseEntity.ok(cohortMaterializationService.getAnnotations(cohortReview, request));
  }

  private org.pmiops.workbench.db.model.Cohort getDbCohort(String workspaceNamespace,
      String workspaceId, Long cohortId) {
    Workspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    org.pmiops.workbench.db.model.Cohort cohort =
        cohortDao.findOne(cohortId);
    if (cohort == null || cohort.getWorkspaceId() != workspace.getWorkspaceId()) {
      throw new NotFoundException(String.format(
          "No cohort with name %s in workspace %s.", cohortId, workspace.getFirecloudName()));
    }
    return cohort;
  }
}
