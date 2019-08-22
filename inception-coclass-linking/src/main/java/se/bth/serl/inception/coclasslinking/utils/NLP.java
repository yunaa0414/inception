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

package se.bth.serl.inception.coclasslinking.utils;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Collection;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;

public class NLP
{
    private static AnalysisEngine aBaseEngine = null;
    private static AnalysisEngine aStemEngine = null;

    public static AnalysisEngine baseAnalysisEngine() throws RecommendationException
    {
        if (aBaseEngine == null) {
            try {
                AggregateBuilder builder = new AggregateBuilder();
                builder.add(createEngineDescription(OpenNlpSegmenter.class));
                builder.add(createEngineDescription(OpenNlpPosTagger.class));
                builder.add(createEngineDescription(SnowballStemmer.class));
                aBaseEngine = builder.createAggregate();
            }
            catch (ResourceInitializationException e) {
                throw new RecommendationException("Could not create base analysis engine.", e);
            }
        }

        return aBaseEngine;
    }
    
    public static Optional<Token> stem(String term) throws RecommendationException {
        Optional<Token> token = Optional.empty();
        
        initStemEngine();
        
        try {
            JCas jCas = JCasFactory.createText(term, "sv");
            SimplePipeline.runPipeline(jCas.getCas(), aStemEngine);
            
            Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
            
            if (tokens.size() > 0) {
                token = tokens.stream().findFirst();
            }
        } catch (UIMAException e) {
            throw new RecommendationException("Could not run stemmer.", e);
        }
        
        return token;
    }
    
    private static void initStemEngine() throws RecommendationException {
        if (aStemEngine == null) {
            try {
                AggregateBuilder builder = new AggregateBuilder();
                builder.add(createEngineDescription(OpenNlpSegmenter.class));
                builder.add(createEngineDescription(SnowballStemmer.class));
                aStemEngine = builder.createAggregate();
            } catch (ResourceInitializationException e) {
                throw new RecommendationException("Could not create stem analysis engine.", e);
            }
        }
    }
}
