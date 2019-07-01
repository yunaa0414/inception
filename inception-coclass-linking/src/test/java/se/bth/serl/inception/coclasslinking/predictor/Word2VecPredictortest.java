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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.recommender.Term;
import se.bth.serl.inception.coclasslinking.util.TestFixture;

public class Word2VecPredictortest
{
    private static TestFixture fix;
    private IPredictor predictor;

    @Mock
    private RecommenderContext recommenderContext;

    @Mock
    private Word2Vec w2vModel;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void setup()
    {
        fix = new TestFixture();
    }

    @Before
    public void createMocks()
    {
        predictor = new Word2VecPredictor(fix.getCoClassModel(), w2vModel);

        when(w2vModel.wordsNearest(anyString(), anyInt()))
                .thenAnswer(i -> fix.getWordsNearest(i.getArgument(0), i.getArgument(1)));

        when(w2vModel.similarity(anyString(), anyString()))
                .thenAnswer(i -> fix.getSimilarity(i.getArgument(0), i.getArgument(1)));
    }

    @Test
    public void testScoreOfKnownTerms()
    {
        for (int i = 0; i < 10000; i++) {
            Term term = fix.getRandomKnownTerm();

            Map<String, Double> result = predictor.score(recommenderContext, term);
            result.forEach((k, v) -> {
                assertTrue("score (" + v + ") should be larger than 0.0", v > 0.0);
                assertTrue("score (" + v + ") should be smaller or equal to 1.0", v <= 1.0);
            });
        }
    }
}
