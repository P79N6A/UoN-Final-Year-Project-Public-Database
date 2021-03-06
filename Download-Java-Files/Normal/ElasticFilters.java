package org.pmiops.workbench.elasticsearch;

import com.google.api.client.util.Sets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;

/**
 * Utility for conversion of Cohort Builder request into Elasticsearch filters. Instances of this
 * class are used internally to track metadata during request processing.
 */
public final class ElasticFilters {

  private static final Logger log = Logger.getLogger(ElasticFilters.class.getName());

  /**
   * Translates a Cohort Builder search request into an Elasticsearch filter.
   */
  public static QueryBuilder fromCohortSearch(CriteriaDao criteriaDao, SearchRequest req) {
    ElasticFilters f = new ElasticFilters(criteriaDao);
    return f.process(req);
  }

  /**
   * A hand-maintained mapping of which criteria trees map to standard vs source concept IDs. This
   * indicates which concept ID field we should be querying against for a SearchRequest on a given
   * tree.
   * TODO(RW-2249): Document or encode this more centrally.
   */
  private static Set<TreeType> STANDARD_TREES = ImmutableSet.of(
      TreeType.SNOMED, TreeType.DRUG, TreeType.MEAS, TreeType.VISIT);
  /**
   * Criteria trees which are coded hierarchically, e.g. parent code "001", child code "001.002".
   * TODO(RW-2249): Why aren't all trees coded this way?
   */
  private static Set<TreeType> HIERARCHICAL_CODE_TREES =
      ImmutableSet.of(TreeType.ICD9, TreeType.ICD10);

  private static Map<String, String> NON_NESTED_FIELDS = ImmutableMap.of(
    TreeSubType.GEN.toString(), "gender_concept_id",
    TreeSubType.RACE.toString(), "race_concept_id",
    TreeSubType.ETH.toString(), "ethnicity_concept_id");

  private final CriteriaDao criteriaDao;

  private boolean processed = false;
  private Map<SearchParameter, Set<Long>> childrenByCriteriaGroup;

  private ElasticFilters(CriteriaDao criteriaDao) {
    this.criteriaDao = criteriaDao;
  }

  private QueryBuilder process(SearchRequest req) {
    Preconditions.checkArgument(!processed);
    childrenByCriteriaGroup = buildCriteriaGroupLookup(req);

    BoolQueryBuilder filter = QueryBuilders.boolQuery();
    for (SearchGroup sg : req.getIncludes()) {
      filter.filter(searchGroupToFilter(sg));
    }
    for (SearchGroup sg : req.getExcludes()) {
      //Only case to use mustNot is when both includes and excludes exist together
      if (req.getIncludes().isEmpty()) {
        filter.filter(searchGroupToFilter(sg));
      } else {
        filter.mustNot(searchGroupToFilter(sg));
      }
    }
    processed = true;
    return filter;
  }

  /**
   * Every criteria ID (one or more concept IDs) in a SearchGroup is effectively OR'd together
   * implemented via a combination of Elastic nested filters and should's. In particular,
   * NUM_OF_OCCURRENCES queries force an independent evaluation of each criteria ID.
   */
  private QueryBuilder searchGroupToFilter(SearchGroup sg) {
    BoolQueryBuilder filter = QueryBuilders.boolQuery();

    for (SearchGroupItem sgi : sg.getItems()) {
      // Modifiers apply to all criteria in this SearchGroupItem, but will be reapplied to each
      // subquery generated for each criteria ID.
      List<QueryBuilder> modFilters = Lists.newArrayList();

      // Note: For now we roll all non-trivial modifiers into one standard clause template. It's
      // conceivable applying the "N occurrences" pattern everywhere may cause performance problems;
      // if so, non-occurrence value modifiers can be split out as a separate template.
      int occurredAtLeast = 1;
      for (Modifier mod : sgi.getModifiers()) {
        switch (mod.getName()) {
          case NUM_OF_OCCURRENCES:
            occurredAtLeast = Integer.parseInt(Iterables.getOnlyElement(mod.getOperands()));
            break;
          case EVENT_DATE:
          case AGE_AT_EVENT:
            modFilters.add(dateModifierToQuery(mod));
            break;
          case ENCOUNTERS:
            modFilters.add(QueryBuilders.termsQuery("events.visit_concept_id", mod.getOperands()));
            break;
          default:
            throw new BadRequestException("Unknown modifier type: " + mod.getName());
        }
      }

      // TODO(freemabd): Handle Blood Pressure and Deceased
      for (SearchParameter param : sgi.getSearchParameters()) {
        String conceptField = "events." + (isStandardConcept(param) ? "concept_id" : "source_concept_id");
        if (isNonNestedSchema(param)) {
          conceptField = NON_NESTED_FIELDS.get(param.getSubtype());
        }
        Set<String> leafConceptIds = toleafConceptIds(ImmutableList.of(param));
        BoolQueryBuilder b = QueryBuilders.boolQuery();
        if (!leafConceptIds.isEmpty()) {
          b.filter(QueryBuilders.termsQuery(conceptField, leafConceptIds));
        }
        for (Attribute attr : param.getAttributes()) {
          b.filter(attributeToQuery(attr));
        }
        for (QueryBuilder f : modFilters) {
          b.filter(f);
        }

        if (isNonNestedSchema(param)) {
          // setup non nested filter with proper field
          filter.should(b);
        } else {
          // "should" gives us "OR" behavior so long as we're in a filter context, which we are. This
          // translates to N occurrences of criteria 1 OR N occurrences of criteria 2, etc.
          filter.should(
            QueryBuilders.functionScoreQuery(
              QueryBuilders.nestedQuery(
                // We sum a constant score for each matching document, yielding the total number
                // of matching nested documents (events).
                "events", QueryBuilders.constantScoreQuery(b), ScoreMode.Total))
              .setMinScore(occurredAtLeast));
        }
      }
    }

    return filter;
  }

  private static QueryBuilder attributeToQuery(Attribute attr) {
    //Attributes with a name of CAT map to the value_as_concept_id column
    if (AttrName.CAT.equals(attr.getName())) {
      //Currently the UI only uses the In operator for CAT which fits the terms query
      return QueryBuilders.termsQuery("events.value_as_concept_id", attr.getOperands());
    }
    Object left = null, right = null;
    RangeQueryBuilder rq;
    if (AttrName.NUM.equals(attr.getName())) {
      rq = QueryBuilders.rangeQuery("events.value_as_number");
      left = Float.parseFloat(attr.getOperands().get(0));
      if (attr.getOperands().size() > 1) {
        right = Float.parseFloat(attr.getOperands().get(1));
      }
      switch (attr.getOperator()) {
        case LESS_THAN_OR_EQUAL_TO:
          rq.lte(left);
          break;
        case GREATER_THAN_OR_EQUAL_TO:
          rq.gte(left);
          break;
        case BETWEEN:
          rq.gte(left).lte(right);
          break;
        case EQUAL:
          rq.gte(left).lte(left);
          break;
        default:
          throw new BadRequestException("Bad operator for attribute: " + attr.getOperator());
      }
      return rq;
    } else if (AttrName.AGE.equals(attr.getName())) {
      rq = QueryBuilders.rangeQuery("birth_datetime");
      //use the low end of the age range to calculate the high end(right) of the date range
      right = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(0)));
      if (attr.getOperands().size() > 1) {
        //use high end of the age range to calculate the low end(left) of the date range
        //need to add 1 year to adjust to the beginning of the date range
        //Ex: 2019-03-19(current date) - 55year(age) - 1 year = 1963-03-19
        //Need to use GT to make sure not to include 1963-03-19 which evaluates to 56 years old
        //which is out the range of 55. 1963-03-20 evaluates to 55 years 11 months 30 days.
        left = ElasticUtils.todayMinusYears(Integer.parseInt(attr.getOperands().get(1)) + 1);
      }
      switch (attr.getOperator()) {
        case BETWEEN:
          rq.gt(left).lte(right).format("yyyy-MM-dd");
          break;
        default:
          throw new BadRequestException("Bad operator for attribute: " + attr.getOperator());
      }
      return rq;
    }
    throw new BadRequestException("attribute name is not an attr name type: " + attr.getName());
  }

  private static QueryBuilder dateModifierToQuery(Modifier mod) {
    RangeQueryBuilder rq;
    Object left, right = null;
    if (ModifierType.EVENT_DATE.equals(mod.getName())) {
      rq = QueryBuilders.rangeQuery("events.start_date");
      left = mod.getOperands().get(0);
      if (mod.getOperands().size() > 1) {
        right = mod.getOperands().get(1);
      }
    } else if (ModifierType.AGE_AT_EVENT.equals(mod.getName())) {
      rq = QueryBuilders.rangeQuery("events.age_at_start");
      left = Integer.parseInt(mod.getOperands().get(0));
      if (mod.getOperands().size() > 1) {
        right = Integer.parseInt(mod.getOperands().get(1));
      }
    } else {
      throw new RuntimeException("modifier is not a date modifier type: " + mod.getName());
    }
    switch (mod.getOperator()) {
      case LESS_THAN:
        rq.lt(left);
        break;
      case GREATER_THAN:
        rq.gt(left);
        break;
      case LESS_THAN_OR_EQUAL_TO:
        rq.lte(left);
        break;
      case GREATER_THAN_OR_EQUAL_TO:
        rq.gte(left);
        break;
      case BETWEEN:
        rq.gte(left).lte(right);
        break;
      case LIKE:
      case IN:
      case EQUAL:
      case NOT_EQUAL:
      default:
        throw new BadRequestException("Bad operator for date modifier: " + mod.getOperator());
    }
    return rq;
  }

  private static boolean isStandardConcept(SearchParameter param) {
    TreeType paramType = TreeType.valueOf(param.getType());
    return STANDARD_TREES.contains(paramType);
  }

  private static boolean isNonNestedSchema(SearchParameter param) {
    TreeType paramType = TreeType.valueOf(param.getType());
    return TreeType.DEMO.equals(paramType);
  }

  private Set<String> toleafConceptIds(List<SearchParameter> params) {
    Set<String> out = Sets.newHashSet();
    for (SearchParameter param : params) {
      if (param.getGroup()) {
        out.addAll(
            childrenByCriteriaGroup.get(param)
                .stream()
                .map(id -> Long.toString(id))
                .collect(Collectors.toSet()));
      } else if (param.getConceptId() != null) {
        //not all SearchParameter have a concept id, so attributes/modifiers
        //are used to find matches in those scenarios.
        out.add(Long.toString(param.getConceptId()));
      }
    }
    return out;
  }

  private static class FullTreeType {
    final TreeType type;
    final TreeSubType subType;
    private FullTreeType(TreeType type, TreeSubType subType) {
      this.type = type;
      this.subType = subType;
    }

    static FullTreeType fromParam(SearchParameter param) {
      return new FullTreeType(
          TreeType.valueOf(param.getType()), TreeSubType.valueOf(param.getSubtype()));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FullTreeType that = (FullTreeType) o;
      return type == that.type &&
          subType == that.subType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, subType);
    }
  }

  /**
   * Extracts all criteria groups in the given search request and produces a lookup map from group
   * parameter to set of matching child concept ids.
   */
  private Map<SearchParameter, Set<Long>> buildCriteriaGroupLookup(SearchRequest req) {
    // Three categories of criteria groups are currently supported in a SearchRequest:
    // 1. Groups with tree types & a concept ID specified, e.g. drugs.
    // 2. Groups with tree types & a criteria code specified, e.g. ICD9/ICD10.
    // 3. Groups with only tree types specified, e.g. PPI surveys (the basics).
    Map<FullTreeType, Map<Long, Set<Long>>> childrenByParentConcept = Maps.newHashMap();
    Map<FullTreeType, Map<String, Set<Long>>> childrenByParentCode = Maps.newHashMap();
    Map<FullTreeType, Set<Long>> childrenByTreeType = Maps.newHashMap();

    // Within each category/tree type combination, we can batch MySQL requests to translate these
    // groups into child concept IDs. First we build up maps to denote which groups to query and
    // which will eventually hold the results (for now, we mark them with an empty set).
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup()) {
            continue;
          }
          if (TreeType.SNOMED.toString().equals(param.getType())) {
            log.warning("Received a SNOMED group in a search request - this indicates a client " +
                "bug. SNOMED concepts are poly-hierarchical; the SearchRequest does not encode " +
                "enough information to determine which criteria ID should be used");
            throw new BadRequestException("Invalid criteria group of type SNOMED");
          }

          FullTreeType treeKey = FullTreeType.fromParam(param);
          if (param.getConceptId() != null) {
            childrenByParentConcept.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByParentConcept.get(treeKey)
                .putIfAbsent(param.getConceptId(), Sets.newHashSet());
          } else if (param.getValue() != null) {
            if (!HIERARCHICAL_CODE_TREES.contains(treeKey.type)) {
              throw new BadRequestException(
                  "Search on criteria group by code is unsupported in tree " + treeKey.type);
            }
            childrenByParentCode.putIfAbsent(treeKey, Maps.newHashMap());
            childrenByParentCode.get(treeKey).putIfAbsent(param.getValue(), Sets.newHashSet());
          } else {
            childrenByTreeType.putIfAbsent(treeKey, Sets.newHashSet());
          }
        }
      }
    }

    // Now we get the child concept IDs for each batch.
    for (FullTreeType treeType : childrenByParentConcept.keySet()) {
      Map<Long, Set<Long>> byParent = childrenByParentConcept.get(treeType);
      Set<String> parentConceptIds =
          byParent.keySet().stream().map(c -> c.toString()).collect(Collectors.toSet());

      List<Criteria> parents = Lists.newArrayList();
      List<Criteria> leaves = Lists.newArrayList();
      criteriaDao.findCriteriaLeavesAndParentsByTypeAndParentConceptIds(
          treeType.type.toString(), treeType.subType.toString(), parentConceptIds)
          .forEach(c -> {
            if (c.getGroup()) {
              parents.add(c);
            } else {
              leaves.add(c);
            }
          });

      for (Criteria c : leaves) {
        // Technically this could scale poorly with many criteria groups. We don't expect this
        // number to be very high as it requires a user action to add a group, but a better data
        // structure could be used here if this becomes too slow.
        for (Criteria parent : parents) {
          String parentId = Long.toString(parent.getId());
          if (c.getPath().startsWith(parentId + ".") ||
              c.getPath().contains("." + parentId + ".") ||
              c.getPath().endsWith("." + parentId)) {
            long parentConceptId = Long.parseLong(parent.getConceptId());
            byParent.putIfAbsent(parentConceptId, Sets.newHashSet());
            byParent.get(parentConceptId).add(Long.parseLong(c.getConceptId()));
          }
        }
      }
    }
    for (FullTreeType treeType : childrenByParentCode.keySet()) {
      Map<String, Set<Long>> byParent = childrenByParentCode.get(treeType);
      List<Criteria> criteriaList = criteriaDao.findCriteriaLeavesByTypeAndParentCodeRegex(
          treeType.type.toString(), treeType.subType.toString(),
          String.format("^(%s)", Strings.join(byParent.keySet(), "|")));
      for (Criteria c : criteriaList) {
        // See above comment on performance, this has the same characteristics.
        for (String parentCode : byParent.keySet()) {
          if (c.getCode().startsWith(parentCode)) {
            byParent.putIfAbsent(parentCode, Sets.newHashSet());
            byParent.get(parentCode).add(Long.parseLong(c.getConceptId()));
          }
        }
      }
    }
    for (FullTreeType treeType : childrenByTreeType.keySet()) {
      childrenByTreeType.get(treeType).addAll(
          criteriaDao.findCriteriaLeavesByType(
              treeType.type.toString(), treeType.subType.toString())
              .stream()
              .map(c -> Long.parseLong(c.getConceptId()))
              .collect(Collectors.toSet()));
    }

    // Finally, we unpack the results and map them back to the original SearchParameters.
    ImmutableMap.Builder<SearchParameter, Set<Long>> builder = ImmutableMap.builder();
    for (SearchGroup sg : Iterables.concat(req.getIncludes(), req.getExcludes())) {
      for (SearchGroupItem sgi : sg.getItems()) {
        for (SearchParameter param : sgi.getSearchParameters()) {
          if (!param.getGroup()) {
            continue;
          }
          FullTreeType treeKey = FullTreeType.fromParam(param);
          if (param.getConceptId() != null) {
            builder.put(param, childrenByParentConcept.get(treeKey).get(param.getConceptId()));
          } else if (param.getValue() != null) {
            builder.put(param, childrenByParentCode.get(treeKey).get(param.getValue()));
          } else {
            builder.put(param, childrenByTreeType.get(treeKey));
          }
        }
      }
    }
    return builder.build();
  }
}
