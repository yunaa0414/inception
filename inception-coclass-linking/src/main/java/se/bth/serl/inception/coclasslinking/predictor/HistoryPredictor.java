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

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;
import se.bth.serl.inception.coclasslinking.recommender.CoClassLinker;
import se.bth.serl.inception.coclasslinking.recommender.CoClassLinker.IriFrequency;
import se.bth.serl.inception.coclasslinking.recommender.Term;

public class HistoryPredictor
    extends PredictorBase
{

    public HistoryPredictor(Map<String, List<CCObject>> aCoClassModel)
    {
        super(aCoClassModel);
    }

    @Override
    public String getName()
    {
        return "History predictor";
    }

    /**
     * Scores are calculated by the frequency a term has been equally and differently annotated
     * annotated
     */
    @Override
    public Map<String, Double> score(RecommenderContext aContext, Term aTerm)
    {
        Map<String, Double> result = new HashMap<>();

        aContext.get(CoClassLinker.KEY_MODEL).ifPresent((model) -> {
            IriFrequency iriFrequency = model.get(aTerm.getTerm());
            if (iriFrequency != null) {
                Map<String, Integer> iris = iriFrequency.getEntries();
                int numberOfHits = iris.size();
                iris.forEach((iri, annotationFrequency) -> {
                    /*
                     * The score is the number of equally annotated terms (annotationFrequency)
                     * divided by the frequency the term has been annotated overall (numberOfHits):
                     * score = annotationFrequency / numberOfHits
                     * 
                     * However, we want a score between 0 and 1. Hence the following scaling: score
                     * = annotationFrequency / (numberOfHits * annotationFrequency)
                     * 
                     * This simplifies to: score = 1.0 / numberOfHits
                     */

                    result.put(iri, 1.0 / numberOfHits);
                });
            }
        });

        return result;
    }
}
