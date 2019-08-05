/*
 * Copyright 2019
 * Software Engineering Research Lab
 * Blekinge Institute of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.bth.serl.inception.coclasslinking.predictor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;
import se.bth.serl.inception.coclasslinking.recommender.CoClassLinker;
import se.bth.serl.inception.coclasslinking.recommender.CoClassLinker.IriFrequency;
import se.bth.serl.inception.coclasslinking.recommender.Term;

public class HistoryPredictor
    extends PredictorBase
{
    private int maximumRejects;
    private List<LearningRecord> learnedRecords;
    private LearningRecordService lrService;
    private AnnotationLayer annLayer;

    public HistoryPredictor(Map<String, List<CCObject>> aCoClassModel, 
            LearningRecordService aLrService, AnnotationLayer aLayer, int aMaximumRejects)
    {
        super(aCoClassModel);
        learnedRecords = null;
        lrService = aLrService;
        annLayer = aLayer;
        maximumRejects = aMaximumRejects;
    }

    @Override
    public String getName()
    {
        return "History predictor";
    }

    /**
     * The score for an IRI is NEGATIVE_INFINITY if the suggestion has been rejected equal or more 
     * than MAXIMUM_REJECTS.
     * However, if a suggestion has been accepted at least once (i.e. it is in the model),
     * the score is calculated by the frequency a term has been equally and differently annotated.
     */
    @Override
    public Map<String, Double> score(RecommenderContext aContext, Term aTerm)
    {
        Map<String, Double> result = new HashMap<>();
        
        /* If there is a limit set for the maximum of rejects:
         * - find learned records with the given term
         * - select the rejected ones
         * - group them by IRI and count them
         * - keep only entries with a count higher or equal to MAXIMUM_REJECTS
         * - add those IRIs to the result with a score of NEGATIVE_INFINITY
         */
        if (maximumRejects > 0) {
            if (learnedRecords == null) {
                retrieveLearnedRecords(aContext);
            }
            
            learnedRecords.stream()
                .filter(r -> r.getTokenText().toLowerCase().equals(aTerm.getTerm()) && 
                    r.getUserAction().equals(LearningRecordType.REJECTED))
                .collect(Collectors.groupingBy(LearningRecord::getAnnotation, 
                    Collectors.counting()))
                .entrySet().stream()
                .filter(r -> r.getValue() >= maximumRejects)
                .forEach(r -> result.put(r.getKey(), Double.NEGATIVE_INFINITY));
        }
        
        aContext.get(CoClassLinker.KEY_MODEL).ifPresent((model) -> {
            IriFrequency iriFrequency = model.get(aTerm.getTerm());
            if (iriFrequency != null) {
                Map<String, Integer> iris = iriFrequency.getEntries();
                int numberOfHits = iris.size();
                iris.forEach((iri, annotationFrequency) -> {
                    /*
                     * The score is the number of equally annotated terms (annotationFrequency)
                     * divided by the frequency the term has been annotated overall 
                     * (numberOfHits):
                     * score = annotationFrequency / numberOfHits
                     * 
                     * However, we want a score between 0 and 1. Hence the following scaling: 
                     * score = annotationFrequency / (numberOfHits * annotationFrequency)
                     * 
                     * This simplifies to: score = 1.0 / numberOfHits
                     */

                    result.put(iri, 1.0 / numberOfHits);
                    
                });
            }
        });

        return result;
    }
    
    private void retrieveLearnedRecords(RecommenderContext ctx) {
        Optional<User> user = ctx.getUser();
        if (user.isPresent()) {
            learnedRecords = lrService.listRecords(user.get().getUsername(), annLayer);
        }
    }
}
