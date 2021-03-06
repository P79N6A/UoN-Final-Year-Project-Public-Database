package org.pmiops.workbench.publicapi;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDistDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResultDist;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.pmiops.workbench.cdr.model.SurveyModule;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.ConceptAnalysis;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.MatchType;
import org.pmiops.workbench.model.QuestionConceptListResponse;
import org.pmiops.workbench.model.ConceptAnalysisListResponse;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.DomainInfosAndSurveyModulesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@RestController
public class DataBrowserController implements DataBrowserApiDelegate {

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private DomainInfoDao domainInfoDao;
    @Autowired
    private SurveyModuleDao surveyModuleDao;
    @Autowired
    private AchillesResultDistDao achillesResultDistDao;
    @PersistenceContext(unitName = "cdr")
    private EntityManager entityManager;
    @Autowired
    @Qualifier("defaultCdr")
    private Provider<CdrVersion> defaultCdrVersionProvider;
    @Autowired
    private ConceptService conceptService;

    public DataBrowserController() {}

    public DataBrowserController(ConceptService conceptService, ConceptDao conceptDao,
        DomainInfoDao domainInfoDao, SurveyModuleDao surveyModuleDao,
        AchillesResultDao achillesResultDao,
        AchillesAnalysisDao achillesAnalysisDao, AchillesResultDistDao achillesResultDistDao,
        EntityManager entityManager, Provider<CdrVersion> defaultCdrVersionProvider) {
        this.conceptService = conceptService;
        this.conceptDao = conceptDao;
        this.domainInfoDao = domainInfoDao;
        this.surveyModuleDao = surveyModuleDao;
        this.achillesResultDao = achillesResultDao;
        this.achillesAnalysisDao = achillesAnalysisDao;
        this.achillesResultDistDao = achillesResultDistDao;
        this.entityManager = entityManager;
        this.defaultCdrVersionProvider = defaultCdrVersionProvider;
    }

    public static final long PARTICIPANT_COUNT_ANALYSIS_ID = 1;
    public static final long COUNT_ANALYSIS_ID = 3000;
    public static final long GENDER_ANALYSIS_ID = 3101;
    public static final long GENDER_IDENTITY_ANALYSIS_ID = 3107;
    public static final long AGE_ANALYSIS_ID = 3102;

    public static final long RACE_ANALYSIS_ID = 3103;
    public static final long ETHNICITY_ANALYSIS_ID = 3104;

    public static final long MEASUREMENT_DIST_ANALYSIS_ID = 1815;

    public static final long MEASUREMENT_GENDER_DIST_ANALYSIS_ID = 1815;
    public static final long MEASUREMENT_AGE_DIST_ANALYSIS_ID = 1816;

    public static final long MEASUREMENT_GENDER_ANALYSIS_ID = 1900;
    public static final long MEASUREMENT_AGE_ANALYSIS_ID = 1901;
    public static final long MEASUREMENT_GENDER_UNIT_ANALYSIS_ID = 1910;

    public static final long MALE = 8507;
    public static final long FEMALE = 8532;
    public static final long INTERSEX = 1585848;
    public static final long NONE = 1585849;
    public static final long OTHER = 0;

    public static final long GENDER_ANALYSIS = 2;
    public static final long RACE_ANALYSIS = 4;
    public static final long ETHNICITY_ANALYSIS = 5;

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<Concept, org.pmiops.workbench.model.Concept>
            TO_CLIENT_CONCEPT =
            new Function<Concept, org.pmiops.workbench.model.Concept>() {
                @Override
                public org.pmiops.workbench.model.Concept apply(Concept concept) {
                    return new org.pmiops.workbench.model.Concept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .standardConcept(concept.getStandardConcept())
                            .conceptCode(concept.getConceptCode())
                            .conceptClassId(concept.getConceptClassId())
                            .vocabularyId(concept.getVocabularyId())
                            .domainId(concept.getDomainId())
                            .countValue(concept.getCountValue())
                            .sourceCountValue(concept.getSourceCountValue())
                            .prevalence(concept.getPrevalence())
                            .conceptSynonyms(concept.getSynonyms());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>
            TO_CLIENT_QUESTION_CONCEPT =
            new Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>() {
                @Override
                public org.pmiops.workbench.model.QuestionConcept apply(QuestionConcept concept) {
                    org.pmiops.workbench.model.Analysis countAnalysis=null;
                    org.pmiops.workbench.model.Analysis genderAnalysis=null;
                    org.pmiops.workbench.model.Analysis ageAnalysis=null;
                    org.pmiops.workbench.model.Analysis genderIdentityAnalysis=null;
                    List<org.pmiops.workbench.model.QuestionConcept> subQuestions = null;
                    if(concept.getCountAnalysis() != null){
                        countAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getCountAnalysis());
                    }
                    if(concept.getGenderAnalysis() != null){
                        genderAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getGenderAnalysis());
                    }
                    if(concept.getAgeAnalysis() != null){
                        ageAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getAgeAnalysis());
                    }
                    if(concept.getGenderIdentityAnalysis() != null){
                        genderIdentityAnalysis = TO_CLIENT_ANALYSIS.apply(concept.getGenderIdentityAnalysis());
                    }
                    if(concept.getSubQuestions() != null) {
                        subQuestions = concept.getSubQuestions().stream().map(TO_CLIENT_QUESTION_CONCEPT).collect(Collectors.toList());
                    }

                    return new org.pmiops.workbench.model.QuestionConcept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .conceptCode(concept.getConceptCode())
                            .domainId(concept.getDomainId())
                            .countValue(concept.getCountValue())
                            .prevalence(concept.getPrevalence())
                            .countAnalysis(countAnalysis)
                            .genderAnalysis(genderAnalysis)
                            .ageAnalysis(ageAnalysis)
                            .genderIdentityAnalysis(genderIdentityAnalysis)
                            .subQuestions(subQuestions);

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>
            TO_CLIENT_ANALYSIS =
            new Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>() {
                @Override
                public org.pmiops.workbench.model.Analysis apply(AchillesAnalysis cdr) {
                    List<org.pmiops.workbench.model.AchillesResult> results = new ArrayList<>();
                    if (!cdr.getResults().isEmpty()) {
                        results = cdr.getResults().stream().map(TO_CLIENT_ACHILLES_RESULT).collect(Collectors.toList());
                    }

                    List<org.pmiops.workbench.model.AchillesResultDist> distResults = new ArrayList<>();
                    if (!cdr.getDistResults().isEmpty()) {
                        distResults = cdr.getDistResults().stream().map(TO_CLIENT_ACHILLES_RESULT_DIST).collect(Collectors.toList());
                    }

                    return new org.pmiops.workbench.model.Analysis()
                            .analysisId(cdr.getAnalysisId())
                            .analysisName(cdr.getAnalysisName())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5Name(cdr.getStratum5Name())
                            .chartType(cdr.getChartType())
                            .dataType(cdr.getDataType())
                            .unitName(cdr.getUnitName())
                            .results(results)
                            .distResults(distResults)
                            .unitName(cdr.getUnitName());

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<ConceptAnalysis, ConceptAnalysis>
            TO_CLIENT_CONCEPTANALYSIS=
            new Function<ConceptAnalysis, ConceptAnalysis>() {
                @Override
                public ConceptAnalysis apply(ConceptAnalysis ca) {
                    return new ConceptAnalysis()
                            .conceptId(ca.getConceptId())
                            .genderAnalysis(ca.getGenderAnalysis())
                            .genderIdentityAnalysis(ca.getGenderIdentityAnalysis())
                            .ageAnalysis(ca.getAgeAnalysis())
                            .raceAnalysis(ca.getRaceAnalysis())
                            .ethnicityAnalysis(ca.getEthnicityAnalysis())
                            .measurementValueGenderAnalysis(ca.getMeasurementValueGenderAnalysis())
                            .measurementValueAgeAnalysis(ca.getMeasurementValueAgeAnalysis())
                            .measurementDistributionAnalysis(ca.getMeasurementDistributionAnalysis())
                            .measurementGenderCountAnalysis(ca.getMeasurementGenderCountAnalysis());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>
            TO_CLIENT_ACHILLES_RESULT =
            new Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>() {
                @Override
                public org.pmiops.workbench.model.AchillesResult apply(AchillesResult o) {

                    return new org.pmiops.workbench.model.AchillesResult()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .analysisStratumName(o.getAnalysisStratumName())
                            .countValue(o.getCountValue())
                            .sourceCountValue(o.getSourceCountValue());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResultDist, org.pmiops.workbench.model.AchillesResultDist>
            TO_CLIENT_ACHILLES_RESULT_DIST =
            new Function<AchillesResultDist, org.pmiops.workbench.model.AchillesResultDist>() {
                @Override
                public org.pmiops.workbench.model.AchillesResultDist apply(AchillesResultDist o) {

                    return new org.pmiops.workbench.model.AchillesResultDist()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .countValue(o.getCountValue())
                            .minValue(o.getMinValue())
                            .maxValue(o.getMaxValue())
                            .avgValue(o.getAvgValue())
                            .stdevValue(o.getStdevValue())
                            .medianValue(o.getMedianValue())
                            .p10Value(o.getP10Value())
                            .p25Value(o.getP25Value())
                            .p75Value(o.getP75Value())
                            .p90Value(o.getP90Value());
                }
            };

    @Override
    public ResponseEntity<DomainInfosAndSurveyModulesResponse> getDomainSearchResults(String query){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        String domainKeyword = ConceptService.modifyMultipleMatchKeyword(query, ConceptService.SearchType.DOMAIN_COUNTS);
        String surveyKeyword = ConceptService.modifyMultipleMatchKeyword(query, ConceptService.SearchType.SURVEY_COUNTS);
        Long conceptId = 0L;
        try {
            conceptId = Long.parseLong(query);
        } catch (NumberFormatException e) {
            // expected
        }
        // TODO: consider parallelizing these lookups
        List<Long> toMatchConceptIds = new ArrayList<>();
        toMatchConceptIds.add(conceptId);
        List<Concept> drugMatchedConcepts = conceptDao.findDrugIngredientsByBrand(query);
        if (drugMatchedConcepts.size() > 0) {
            toMatchConceptIds.addAll(drugMatchedConcepts.stream().map(Concept::getConceptId).collect(Collectors.toList()));
        }

        List<DomainInfo> domains = domainInfoDao.findStandardOrCodeMatchConceptCounts(domainKeyword, query, toMatchConceptIds);
        List<SurveyModule> surveyModules = surveyModuleDao.findSurveyModuleQuestionCounts(surveyKeyword);
        DomainInfosAndSurveyModulesResponse response = new DomainInfosAndSurveyModulesResponse();
        response.setDomainInfos(domains.stream()
            .map(DomainInfo.TO_CLIENT_DOMAIN_INFO)
            .collect(Collectors.toList()));
        response.setSurveyModules(surveyModules.stream()
            .map(SurveyModule.TO_CLIENT_SURVEY_MODULE)
            .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConceptListResponse> searchConcepts(SearchConceptsRequest searchConceptsRequest){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        Integer maxResults = searchConceptsRequest.getMaxResults();
        if(maxResults == null || maxResults == 0){
            maxResults = Integer.MAX_VALUE;
        }

        Integer minCount = searchConceptsRequest.getMinCount();
        if(minCount == null){
            minCount = 1;
        }

        StandardConceptFilter standardConceptFilter = searchConceptsRequest.getStandardConceptFilter();

        if(searchConceptsRequest.getQuery() == null || searchConceptsRequest.getQuery().isEmpty()){
            if(standardConceptFilter == null || standardConceptFilter == StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH){
                standardConceptFilter = StandardConceptFilter.STANDARD_CONCEPTS;
            }
        }else{
            if(standardConceptFilter == null){
                standardConceptFilter = StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH;
            }
        }

        List<String> domainIds = null;
        if (searchConceptsRequest.getDomain() != null) {
            domainIds = ImmutableList.of(CommonStorageEnums.domainToDomainId(searchConceptsRequest.getDomain()));
        }

        ConceptService.StandardConceptFilter convertedConceptFilter = ConceptService.StandardConceptFilter.valueOf(standardConceptFilter.name());

        Slice<Concept> concepts = null;
        concepts = conceptService.searchConcepts(searchConceptsRequest.getQuery(), convertedConceptFilter,
                searchConceptsRequest.getVocabularyIds(), domainIds, maxResults, minCount);
        ConceptListResponse response = new ConceptListResponse();

        for(Concept con : concepts.getContent()){
            String conceptCode = con.getConceptCode();
            String conceptId = String.valueOf(con.getConceptId());

            if((con.getStandardConcept() == null || !con.getStandardConcept().equals("S") ) && (searchConceptsRequest.getQuery().equals(conceptCode) || searchConceptsRequest.getQuery().equals(conceptId))){
                response.setMatchType(conceptCode.equals(searchConceptsRequest.getQuery()) ? MatchType.CODE : MatchType.ID );

                List<Concept> stdConcepts = conceptDao.findStandardConcepts(con.getConceptId());
                response.setStandardConcepts(stdConcepts.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));

                List<Concept> tempSourceConcepts = new ArrayList<>();
                tempSourceConcepts.add(con);
                response.setItems(tempSourceConcepts.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
                return ResponseEntity.ok(response);
            }

        }

        if(response.getMatchType() == null && response.getStandardConcepts() == null){
            response.setMatchType(MatchType.NAME);
        }

        List<Concept> conceptList = new ArrayList(concepts.getContent());
        if(searchConceptsRequest.getDomain() != null && searchConceptsRequest.getDomain().equals(Domain.DRUG) && !searchConceptsRequest.getQuery().isEmpty()) {
            List<Concept> drugMatchedConcepts = conceptDao.findDrugIngredientsByBrand(searchConceptsRequest.getQuery());

            if(drugMatchedConcepts.size() > 0) {
                conceptList.addAll(drugMatchedConcepts);
            }
        }

        response.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DomainInfosAndSurveyModulesResponse> getDomainTotals(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        List<DomainInfo> domainInfos = ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId());
        List<SurveyModule> surveyModules = ImmutableList.copyOf(surveyModuleDao.findByOrderByOrderNumberAsc());
        DomainInfosAndSurveyModulesResponse response = new DomainInfosAndSurveyModulesResponse();
        response.setDomainInfos(domainInfos.stream()
            .map(DomainInfo.TO_CLIENT_DOMAIN_INFO)
            .collect(Collectors.toList()));
        response.setSurveyModules(surveyModules.stream()
            .map(SurveyModule.TO_CLIENT_SURVEY_MODULE)
            .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getGenderAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis genderAnalysis = achillesAnalysisDao.findAnalysisById(GENDER_ANALYSIS);
        addGenderStratum(genderAnalysis);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(genderAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getRaceAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis raceAnalysis = achillesAnalysisDao.findAnalysisById(RACE_ANALYSIS);
        addRaceStratum(raceAnalysis);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(raceAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getEthnicityAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis ethnicityAnalysis = achillesAnalysisDao.findAnalysisById(ETHNICITY_ANALYSIS);
        addEthnicityStratum(ethnicityAnalysis);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(ethnicityAnalysis));
    }

    @Override
    public ResponseEntity<QuestionConceptListResponse> getSurveyResults(String surveyConceptId) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        /* Set up the age and gender names */
        // Too slow and concept names wrong so we hardcode list
        // List<Concept> genders = conceptDao.findByConceptClassId("Gender");

        //Get the list of questions that has sub questions in survey
        Set<Long> hasSubQuestions = questionConceptDao.findSurveyMainQuestionIds(Long.valueOf(surveyConceptId)).stream().map(QuestionConcept::getConceptId).collect(Collectors.toSet());

        long longSurveyConceptId = Long.parseLong(surveyConceptId);

        // Get questions for survey
        List<QuestionConcept> questions = questionConceptDao.findSurveyQuestions(surveyConceptId);

        // Get survey definition
        QuestionConceptListResponse resp = new QuestionConceptListResponse();

        SurveyModule surveyModule = surveyModuleDao.findByConceptId(longSurveyConceptId);

        resp.setSurvey(SurveyModule.TO_CLIENT_SURVEY_MODULE.apply(surveyModule));
        // Get all analyses for question list and put the analyses on the question objects
        if (!questions.isEmpty()) {
            // Put ids in array for query to get all results at once
            List<String> qlist = new ArrayList();
            for (QuestionConcept q : questions) {
                if (hasSubQuestions.contains(q.getConceptId())) {
                    List<QuestionConcept> subQuestions = questionConceptDao.findSubSurveyQuestions(surveyConceptId, q.getConceptId());
                    QuestionConcept.mapAnalysesToQuestions(subQuestions, achillesAnalysisDao.findSurveyAnalysisResults(surveyConceptId, subQuestions.stream()
                            .map(QuestionConcept::getConceptId)
                            .map(String::valueOf)
                            .collect(Collectors.toList())));
                    q.setSubQuestions(subQuestions);
                }
                qlist.add(String.valueOf(q.getConceptId()));
            }

            List<AchillesAnalysis> analyses = achillesAnalysisDao.findSurveyAnalysisResults(surveyConceptId, qlist);
            QuestionConcept.mapAnalysesToQuestions(questions, analyses);
        }

        resp.setItems(questions.stream().map(TO_CLIENT_QUESTION_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<ConceptAnalysisListResponse> getConceptAnalysisResults(List<String> conceptIds, String domainId){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        ConceptAnalysisListResponse resp=new ConceptAnalysisListResponse();
        List<ConceptAnalysis> conceptAnalysisList=new ArrayList<>();
        List<Long> analysisIds  = new ArrayList<>();
        analysisIds.add(GENDER_ANALYSIS_ID);
        analysisIds.add(GENDER_IDENTITY_ANALYSIS_ID);
        analysisIds.add(AGE_ANALYSIS_ID);
        analysisIds.add(RACE_ANALYSIS_ID);
        analysisIds.add(COUNT_ANALYSIS_ID);
        analysisIds.add(ETHNICITY_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_GENDER_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_AGE_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_DIST_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_GENDER_UNIT_ANALYSIS_ID);

        List<AchillesResultDist> overallDistResults = achillesResultDistDao.fetchByAnalysisIdsAndConceptIds(new ArrayList<Long>( Arrays.asList(MEASUREMENT_GENDER_DIST_ANALYSIS_ID,MEASUREMENT_AGE_DIST_ANALYSIS_ID) ),conceptIds);

        Multimap<Long, AchillesResultDist> distResultsByAnalysisId = null;
        if(overallDistResults != null){
            distResultsByAnalysisId = Multimaps
                    .index(overallDistResults, AchillesResultDist::getAnalysisId);
        }

        HashMap<Long,HashMap<String,List<AchillesResultDist>>> analysisDistResults = new HashMap<>();

        for(Long key:distResultsByAnalysisId.keySet()){
            Multimap<String,AchillesResultDist> conceptDistResults = Multimaps.index(distResultsByAnalysisId.get(key),AchillesResultDist::getStratum1);
            for(String concept:conceptDistResults.keySet()) {
                if(analysisDistResults.containsKey(key)){
                    HashMap<String,List<AchillesResultDist>> results = analysisDistResults.get(key);
                    results.put(concept,new ArrayList<>(conceptDistResults.get(concept)));
                }else{
                    HashMap<String,List<AchillesResultDist>> results = new HashMap<>();
                    results.put(concept,new ArrayList<>(conceptDistResults.get(concept)));
                    analysisDistResults.put(key,results);
                }
            }
        }
        for(String conceptId: conceptIds){
            ConceptAnalysis conceptAnalysis=new ConceptAnalysis();

            boolean isMeasurement = false;

            List<AchillesAnalysis> analysisList = achillesAnalysisDao.findConceptAnalysisResults(conceptId,analysisIds);

            HashMap<Long, AchillesAnalysis> analysisHashMap = new HashMap<>();
            for(AchillesAnalysis aa: analysisList){
                this.entityManager.detach(aa);
                analysisHashMap.put(aa.getAnalysisId(), aa);
            }

            conceptAnalysis.setConceptId(conceptId);
            Iterator it = analysisHashMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                Long analysisId = (Long)pair.getKey();
                AchillesAnalysis aa = (AchillesAnalysis)pair.getValue();
                //aa.setUnitName(unitName);
                if(analysisId != MEASUREMENT_GENDER_UNIT_ANALYSIS_ID && analysisId != MEASUREMENT_GENDER_ANALYSIS_ID && analysisId != MEASUREMENT_AGE_ANALYSIS_ID && analysisId != MEASUREMENT_DIST_ANALYSIS_ID && analysisId != MEASUREMENT_AGE_DIST_ANALYSIS_ID && !Strings.isNullOrEmpty(domainId)) {
                    aa.setResults(aa.getResults().stream().filter(ar -> ar.getStratum3().equalsIgnoreCase(domainId)).collect(Collectors.toList()));
                }
                if(analysisId == GENDER_ANALYSIS_ID){
                    addGenderStratum(aa);
                    conceptAnalysis.setGenderAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == GENDER_IDENTITY_ANALYSIS_ID){
                    addGenderIdentityStratum(aa);
                    conceptAnalysis.setGenderIdentityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == AGE_ANALYSIS_ID){
                    addAgeStratum(aa, conceptId);
                    conceptAnalysis.setAgeAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == RACE_ANALYSIS_ID){
                    addRaceStratum(aa);
                    conceptAnalysis.setRaceAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == ETHNICITY_ANALYSIS_ID){
                    addEthnicityStratum(aa);
                    conceptAnalysis.setEthnicityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == MEASUREMENT_GENDER_ANALYSIS_ID){
                    Map<String,List<AchillesResult>> results = seperateUnitResults(aa);
                    List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                    HashMap<String,List<AchillesResultDist>> distResults = analysisDistResults.get(MEASUREMENT_GENDER_DIST_ANALYSIS_ID);
                    if (distResults != null) {
                        List<AchillesResultDist> conceptDistResults = distResults.get(conceptId);
                        if(conceptDistResults != null){
                            Multimap<String,AchillesResultDist> unitDistResults = Multimaps.index(conceptDistResults,AchillesResultDist::getStratum2);
                            for(String unit: unitDistResults.keySet()){
                                if (results.keySet().contains(unit)) {
                                    AchillesAnalysis unitGenderAnalysis = new AchillesAnalysis(aa);
                                    unitGenderAnalysis.setResults(results.get(unit));
                                    unitGenderAnalysis.setUnitName(unit);
                                    if(!unit.equalsIgnoreCase("no unit")) {
                                        processMeasurementGenderMissingBins(MEASUREMENT_GENDER_DIST_ANALYSIS_ID,unitGenderAnalysis, conceptId, unit, new ArrayList<>(unitDistResults.get(unit)));
                                    }
                                    unitSeperateAnalysis.add(unitGenderAnalysis);
                                }
                            }
                        }else {
                            unitSeperateAnalysis.add(aa);
                        }
                    }
                    isMeasurement = true;
                    conceptAnalysis.setMeasurementValueGenderAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
                }else if(analysisId == MEASUREMENT_AGE_ANALYSIS_ID){
                    Map<String,List<AchillesResult>> results = seperateUnitResults(aa);
                    List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                    HashMap<String,List<AchillesResultDist>> distResults = analysisDistResults.get(MEASUREMENT_AGE_DIST_ANALYSIS_ID);
                    if (distResults != null) {
                        List<AchillesResultDist> conceptDistResults = distResults.get(conceptId);
                        if(conceptDistResults != null) {
                            Multimap<String,AchillesResultDist> unitDistResults = Multimaps.index(conceptDistResults,AchillesResultDist::getStratum2);
                            for(String unit: results.keySet()){
                                AchillesAnalysis unitAgeAnalysis = new AchillesAnalysis(aa);
                                unitAgeAnalysis.setResults(results.get(unit));
                                unitAgeAnalysis.setUnitName(unit);
                                if(!unit.equalsIgnoreCase("no unit")) {
                                    processMeasurementAgeDecileMissingBins(MEASUREMENT_AGE_DIST_ANALYSIS_ID,unitAgeAnalysis, conceptId, unit, new ArrayList<>(unitDistResults.get(unit)));
                                }
                                addAgeStratum(unitAgeAnalysis,conceptId);
                                unitSeperateAnalysis.add(unitAgeAnalysis);
                            }
                        }else {
                                unitSeperateAnalysis.add(aa);
                        }
                    }
                    isMeasurement = true;
                    conceptAnalysis.setMeasurementValueAgeAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
                }else if(analysisId == MEASUREMENT_GENDER_UNIT_ANALYSIS_ID){
                    Map<String,List<AchillesResult>> results = seperateUnitResults(aa);
                    List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                    for(String unit: results.keySet()){
                        AchillesAnalysis unitGenderCountAnalysis = new AchillesAnalysis(aa);
                        unitGenderCountAnalysis.setResults(results.get(unit));
                        unitGenderCountAnalysis.setUnitName(unit);
                        unitSeperateAnalysis.add(unitGenderCountAnalysis);
                    }
                    isMeasurement = true;
                    conceptAnalysis.setMeasurementGenderCountAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
                }
            }

            if(isMeasurement){
                AchillesAnalysis measurementDistAnalysis = achillesAnalysisDao.findAnalysisById(MEASUREMENT_DIST_ANALYSIS_ID);
                List<AchillesResultDist> achillesResultDistList = achillesResultDistDao.fetchConceptDistResults(MEASUREMENT_DIST_ANALYSIS_ID,conceptId);
                HashMap<String,List<AchillesResultDist>> results = seperateDistResultsByUnit(achillesResultDistList);
                List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                for(String unit: results.keySet()){
                    AchillesAnalysis mDistAnalysis = new AchillesAnalysis(measurementDistAnalysis);
                    mDistAnalysis.setDistResults(results.get(unit));
                    mDistAnalysis.setUnitName(unit);
                    unitSeperateAnalysis.add(mDistAnalysis);
                }
                conceptAnalysis.setMeasurementDistributionAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
            }
            conceptAnalysisList.add(conceptAnalysis);
        }
        resp.setItems(conceptAnalysisList.stream().map(TO_CLIENT_CONCEPTANALYSIS).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }


    /**
     * This method gets concepts with maps to relationship in concept relationship table
     *
     * @param conceptId
     * @return
     */
    @Override
    public ResponseEntity<ConceptListResponse> getSourceConcepts(Long conceptId,Integer minCount) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        Integer count=minCount;
        if(count == null){
            count = 0;
        }
        List<Concept> conceptList = conceptDao.findSourceConcepts(conceptId,count);
        ConceptListResponse resp = new ConceptListResponse();
        resp.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.AchillesResult> getParticipantCount() {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesResult result = achillesResultDao.findAchillesResultByAnalysisId(PARTICIPANT_COUNT_ANALYSIS_ID);
        return ResponseEntity.ok(TO_CLIENT_ACHILLES_RESULT.apply(result));
    }

    public TreeSet<Float> makeBins(Float min,Float max) {
        TreeSet<Float> bins = new TreeSet<>();
        float binWidth = (max-min)/11;
        bins.add(Float.valueOf(String.format("%.2f", min+binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+2*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+3*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+4*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+5*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+6*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+7*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+8*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+9*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+10*binWidth)));
        bins.add(max);
        return bins;
    }

    public void addGenderStratum(AchillesAnalysis aa){
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName =ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addGenderIdentityStratum(AchillesAnalysis aa){
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName =ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.genderIdentityStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addAgeStratum(AchillesAnalysis aa, String conceptId){
        Set<String> uniqueAgeDeciles = new TreeSet<String>();
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            uniqueAgeDeciles.add(ar.getStratum2());
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
            }
        }
        aa.setResults(aa.getResults().stream().filter(ar -> ar.getAnalysisStratumName() != null).collect(Collectors.toList()));
        if(uniqueAgeDeciles.size() < 7){
            Set<String> completeAgeDeciles = new TreeSet<String>(Arrays.asList(new String[] {"2", "3", "4", "5", "6", "7", "8"}));
            completeAgeDeciles.removeAll(uniqueAgeDeciles);
            for(String missingAgeDecile: completeAgeDeciles){
                AchillesResult missingResult = new AchillesResult(AGE_ANALYSIS_ID, conceptId, missingAgeDecile, null, null, null, 0L, 0L);
                missingResult.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(missingAgeDecile));
                aa.getResults().add(missingResult);
            }
        }
    }

    public void addRaceStratum(AchillesAnalysis aa) {
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                   ar.setAnalysisStratumName(QuestionConcept.raceStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addEthnicityStratum(AchillesAnalysis aa) {
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.raceStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void processMeasurementGenderMissingBins(Long analysisId, AchillesAnalysis aa, String conceptId, String unitName, List<AchillesResultDist> resultDists) {

        Float maleBinMin = null;
        Float maleBinMax = null;

        Float femaleBinMin = null;
        Float femaleBinMax = null;

        Float intersexBinMin = null;
        Float intersexBinMax = null;

        Float noneBinMin = null;
        Float noneBinMax = null;

        Float otherBinMin = null;
        Float otherBinMax = null;

        for(AchillesResultDist ard:resultDists){
            if(Integer.parseInt(ard.getStratum3())== MALE) {
                maleBinMin = Float.valueOf(ard.getStratum4());
                maleBinMax = Float.valueOf(ard.getStratum5());
            }
            else if(Integer.parseInt(ard.getStratum3()) == FEMALE) {
                femaleBinMin = Float.valueOf(ard.getStratum4());
                femaleBinMax = Float.valueOf(ard.getStratum5());
            }
            else if(Integer.parseInt(ard.getStratum3()) == INTERSEX) {
                intersexBinMin = Float.valueOf(ard.getStratum4());
                intersexBinMax = Float.valueOf(ard.getStratum5());
            }
            else if(Integer.parseInt(ard.getStratum3()) == NONE) {
                noneBinMin = Float.valueOf(ard.getStratum4());
                noneBinMax = Float.valueOf(ard.getStratum5());
            }
            else if(Integer.parseInt(ard.getStratum3()) == OTHER) {
                otherBinMin = Float.valueOf(ard.getStratum4());
                otherBinMax = Float.valueOf(ard.getStratum5());
            }
        }


        TreeSet<Float> maleBinRanges = new TreeSet<Float>();
        TreeSet<Float> femaleBinRanges = new TreeSet<Float>();
        TreeSet<Float> intersexBinRanges = new TreeSet<Float>();
        TreeSet<Float> noneBinRanges = new TreeSet<Float>();
        TreeSet<Float> otherBinRanges = new TreeSet<Float>();

        if(maleBinMax != null && maleBinMin != null){
            maleBinRanges = makeBins(maleBinMin, maleBinMax);
        }

        if(femaleBinMax != null && femaleBinMin != null){
            femaleBinRanges = makeBins(femaleBinMin, femaleBinMax);
        }

        if(intersexBinMax != null && intersexBinMin != null){
            intersexBinRanges = makeBins(intersexBinMin, intersexBinMax);
        }

        if(noneBinMax != null && noneBinMin != null){
            noneBinRanges = makeBins(noneBinMin, noneBinMax);
        }

        if(otherBinMax != null && otherBinMin != null){
            otherBinRanges = makeBins(otherBinMin, otherBinMax);
        }

        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if(Long.valueOf(ar.getStratum3()) == MALE && maleBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                maleBinRanges.remove(Float.parseFloat(ar.getStratum4()));
            }else if(Long.valueOf(ar.getStratum3()) == FEMALE && femaleBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                femaleBinRanges.remove(Float.parseFloat(ar.getStratum4()));
            }else if(Long.valueOf(ar.getStratum3()) == INTERSEX && intersexBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                intersexBinRanges.remove(Float.parseFloat(ar.getStratum4()));
            }else if(Long.valueOf(ar.getStratum3()) == NONE && noneBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                noneBinRanges.remove(Float.parseFloat(ar.getStratum4()));
            }else if(Long.valueOf(ar.getStratum3()) == OTHER && otherBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                otherBinRanges.remove(Float.parseFloat(ar.getStratum4()));
            }
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
            }
        }

        for(float maleRemaining: maleBinRanges){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(MALE), String.valueOf(maleRemaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }

        for(float femaleRemaining: femaleBinRanges){
            AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(FEMALE), String.valueOf(femaleRemaining), null, 0L, 0L);
            aa.addResult(ar);
        }

        for(float intersexRemaining: intersexBinRanges){
            AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(INTERSEX), String.valueOf(intersexRemaining), null, 0L, 0L);
            aa.addResult(ar);
        }

        for(float noneRemaining: noneBinRanges){
            AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(NONE), String.valueOf(noneRemaining), null, 0L, 0L);
            aa.addResult(ar);
        }

        for(float otherRemaining: otherBinRanges){
            AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(OTHER), String.valueOf(otherRemaining), null, 0L, 0L);
            aa.addResult(ar);
        }

    }

    public static HashMap<String,List<AchillesResult>> seperateUnitResults(AchillesAnalysis aa){
        List<String> distinctUnits = new ArrayList<>();
        for(AchillesResult ar:aa.getResults()){
            if(!distinctUnits.contains(ar.getStratum2()) && !Strings.isNullOrEmpty(ar.getStratum2())){
                distinctUnits.add(ar.getStratum2());
            }
        }
        Multimap<String, AchillesResult> resultsWithUnits = Multimaps
                .index(aa.getResults(), AchillesResult::getStratum2);
        HashMap<String,List<AchillesResult>> seperatedResults = new HashMap<>();
        for(String key:resultsWithUnits.keySet()){
            seperatedResults.put(key,new ArrayList<>(resultsWithUnits.get(key)));
        }
        return seperatedResults;
    }

    public static HashMap<String,List<AchillesResultDist>> seperateDistResultsByUnit(List<AchillesResultDist> results) {
        Multimap<String, AchillesResultDist> distResultsWithUnits = Multimaps
                .index(results, AchillesResultDist::getStratum2);
        HashMap<String,List<AchillesResultDist>> seperatedResults = new HashMap<>();

        for(String key:distResultsWithUnits.keySet()){
            seperatedResults.put(key,new ArrayList<>(distResultsWithUnits.get(key)));
        }

        return seperatedResults;
    }

    public void processMeasurementAgeDecileMissingBins(Long analysisId, AchillesAnalysis aa, String conceptId, String unitNam, List<AchillesResultDist> distRows) {

        HashMap<String,ArrayList<Float>>  decileRanges = new HashMap<>();

        for(AchillesResultDist ard:distRows){
            if(Integer.parseInt(ard.getStratum3())== '2') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("2",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '3') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("3",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '4') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("4",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '5') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("5",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '6') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("6",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '7') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("7",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '8') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("8",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
        }

        TreeSet<Float> binRanges2 = new TreeSet<Float>();
        TreeSet<Float> binRanges3 = new TreeSet<Float>();
        TreeSet<Float> binRanges4 = new TreeSet<Float>();
        TreeSet<Float> binRanges5 = new TreeSet<Float>();
        TreeSet<Float> binRanges6 = new TreeSet<Float>();
        TreeSet<Float> binRanges7 = new TreeSet<Float>();
        TreeSet<Float> binRanges8 = new TreeSet<Float>();


        if(decileRanges.get("2") != null){
            binRanges2 = makeBins(decileRanges.get("2").get(0), decileRanges.get("2").get(1));
        }
        if(decileRanges.get("3") != null){
            binRanges3 = makeBins(decileRanges.get("3").get(0), decileRanges.get("3").get(1));
        }
        if(decileRanges.get("4") != null){
            binRanges4 = makeBins(decileRanges.get("4").get(0), decileRanges.get("4").get(1));
        }
        if(decileRanges.get("5") != null){
            binRanges5 = makeBins(decileRanges.get("5").get(0), decileRanges.get("5").get(1));
        }
        if(decileRanges.get("6") != null){
            binRanges6 = makeBins(decileRanges.get("6").get(0), decileRanges.get("6").get(1));
        }
        if(decileRanges.get("7") != null){
            binRanges7 = makeBins(decileRanges.get("7").get(0), decileRanges.get("7").get(1));
        }
        if(decileRanges.get("8") != null){
            binRanges8 = makeBins(decileRanges.get("8").get(0), decileRanges.get("8").get(1));
        }

        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if(ar.getStratum2().equals("2") && binRanges2.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges2.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("3") && binRanges3.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges3.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("4") && binRanges4.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges4.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("5") && binRanges5.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges5.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("6") && binRanges6.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges6.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("7") && binRanges7.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges7.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("8") && binRanges8.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges8.remove(Float.parseFloat(ar.getStratum4()));
            }

            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
            }
        }

        for(float remaining: binRanges2){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "2", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
        for(float remaining: binRanges3){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "3", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }

        for(float remaining: binRanges4){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "4", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
        for(float remaining: binRanges5){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "5", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
        for(float remaining: binRanges6){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "6", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
        for(float remaining: binRanges7){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "7", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
        for(float remaining: binRanges8){
            AchillesResult achillesResult = new AchillesResult(MEASUREMENT_AGE_ANALYSIS_ID, conceptId, "8", null, String.valueOf(remaining), null, 0L, 0L);
            aa.addResult(achillesResult);
        }
    }
}
