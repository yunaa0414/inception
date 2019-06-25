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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.deeplearning4j.models.word2vec.Word2Vec;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;
import se.bth.serl.inception.coclasslinking.recommender.Term;

public class Word2VecPredictor extends PredictorBase {
	private static Map<String, Collection<String>> wordsNearestCache = null;
	private static Map<String, Double> similarityCache = null;
	private Word2Vec w2vModel;
	private final int numSimilarWords = 10;
	
	/*
	 * Using pre-trained model: https://github.com/Kyubyong/wordvectors
	 * In order to use it in D4J, I had to load it in python first:
	 * >>> from gensim.models import Word2Vec
	 * >>> model = Word2Vec.load('/home/mun/nosync/word2vec/kyubyong/sv.bin')
	 * and then save in a new file:
	 * https://stackoverflow.com/a/51293410/2091625
	 * 
	 * There are a couple of other larger pre-trained models:
	 * http://vectors.nlpl.eu/repository/
	 * https://fasttext.cc/docs/en/crawl-vectors.html
	 * 
	 * We can also train our own model with the wikipedia and the spearfishing corpus.
	 */
	public Word2VecPredictor(Map<String, List<CCObject>> aCoClassModel, Word2Vec aW2vModel) {
		super(aCoClassModel);
		if (wordsNearestCache == null) {
			wordsNearestCache = new HashMap<>();
		}
		if (similarityCache == null) {
			similarityCache = new HashMap<>();
		}
		w2vModel = aW2vModel;
	}
	
	@Override
	public String getName() {
		return "Word2Vec predictor";
	}
	
	@Override
	public Map<String, Double> score(RecommenderContext aContext, Term aTerm) {
		Map<String, Double> result = new HashMap<>();
				
		String word = aTerm.getTerm();
		
		Collection<String> similarWords = wordsNearestCache.get(word);
		if (similarWords == null) {
			similarWords = w2vModel.wordsNearest(word, numSimilarWords);
			wordsNearestCache.put(word, similarWords);
		}
			
		Map<String, List<CCObject>> hits = new HashMap<>();
		for (String similarWord : similarWords) {
			List<CCObject> ccObjects = coClassModel.get(similarWord.toLowerCase());
			if (ccObjects != null) {
				hits.put(similarWord, ccObjects);
			}
		}
		
		hits.forEach( (similarWord, ccObjects) -> {
			int numberOfHits = ccObjects.size();
			for (CCObject hit : ccObjects) {
				hit.addW2VSimilarWord(similarWord);
					
				/*
				 * A CoClass object can be associated with several similar words. Hence, we need to find these
				 * in all found CoClass objects, count them and add them to the number of hits for the current 
				 * similar word in order to normalize correctly (otherwise, we may end up with scores > 1).
				 */
				numberOfHits += hits.values().stream()
					.flatMap(List::stream)
					.collect(Collectors.toList())
					.stream()
					.filter(p -> p.equals(hit))
					.count() - 1; // substract 1 as list contains also the hit

			
				String simKey = word + similarWord;
				Double newScore = similarityCache.get(simKey);
				if (newScore == null) {
					newScore = new Double(1.0 / numberOfHits * w2vModel.similarity(word, similarWord));
					similarityCache.put(simKey, newScore);
				}
					
				result.merge(hit.getIri(), newScore, (score, increment) -> score += increment);
			}
		});
				
		return result;
	}
}
