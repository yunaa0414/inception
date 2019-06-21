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

package se.bth.serl.inception.coclasslinking.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javafaker.Faker;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;

import se.bth.serl.inception.coclasslinking.recommender.CCObject;
import se.bth.serl.inception.coclasslinking.recommender.Term;

public class TestFixture {
	private static final int W2V_MODEL_SIZE = 1000;
	private static final double COCLASS_MODEL_TERMS_FACTOR = 0.3;
	private static final int COCLASS_MODEL_MAX_TERM_SHARE = 5;
	private static final int TERM_LENGTH = 10;
	
	private Set<String> objectTerms;
	private String uniqueTerm;
	private Table<String, String, Double> w2vModel;
	private Map<String, List<CCObject>> coClassModel;
	private Faker faker;
	
	public TestFixture() {
		objectTerms = new HashSet<>(W2V_MODEL_SIZE);
		faker = new Faker();
		
		while (objectTerms.size() < W2V_MODEL_SIZE) {
			objectTerms.add(generateTerm(0));	
		}
		
		createw2vModel();
		createCoClassModel();
	}
	
	public Map<String, List<CCObject>> getCoClassModel() {
		return coClassModel;
	}
	
	public Collection<String> getWordsNearest(String word, int amount) {
		Map<String, Double> result = w2vModel.row(word).entrySet().stream()
				.filter(p -> p.getValue().doubleValue() != 1.0)
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.limit(amount)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
						(oldValue, newValue) -> oldValue, LinkedHashMap::new));
				
		
		assert(result.size() == amount);
		
		Double previous = Double.MAX_VALUE;
		for (Double r : result.values()) {
			assert(previous.doubleValue() >= r.doubleValue());
			previous = r;
		}
		
		return result.keySet();
	}
	
	public Double getSimilarity(String word1, String word2) {
		Double result = w2vModel.get(word1, word2);
		assert(result != null);
		
		return result;
	}
	
	/*
	 * Generate a random term that is neither in the word2vec nor the CoClass model.
	 */
	public Term generateUnknownTerm() {
		String t = generateTerm(5);
		assert(!w2vModel.rowKeySet().contains(t));
		assert(!coClassModel.containsKey(t));
		return new Term(t);
	}
	
	/*
	 * Get a random term from the CoClass model
	 */
	public Term getRandomKnownTerm() {
		int index = faker.number().numberBetween(0, coClassModel.size() - 1);
		return new Term((String) coClassModel.keySet().toArray()[index]);
	}
	
	/*
	 * Get a term that appears only in one CoClass object
	 */
	public Term getUniqueTerm() {
		return new Term(uniqueTerm);
	}
	
	private String generateTerm(int additionalLength) {
		int length = TERM_LENGTH + additionalLength;
		return faker.regexify("[a-z]{" + length + "}");
	}
	
	private List<String> generateRandomTerms() {
		int amount = faker.number().numberBetween(1, 15);
		List<String> terms = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			terms.add(generateTerm(1));
		}
		
		return terms;
	}
	
	/* The model is a table with random terms as rows/columns and random values
	 * in cells. The value at any row_n/column_n is 1. The table is symmetric at
	 * the row_n/column_n diagonal.
	 */
	private void createw2vModel() {
		List<String> visitedTerms = new ArrayList<>();
	
		w2vModel = ArrayTable.create(objectTerms, objectTerms);
		objectTerms.forEach( term -> {
			w2vModel.put(term, term, 1.0);
			
			for (String visitedTerm : visitedTerms) {
				Double value = faker.number().randomDouble(5, 0, 1);
				w2vModel.put(term, visitedTerm, value);
				w2vModel.put(visitedTerm, term, value);
			}
			
			visitedTerms.add(term);
		});
	}
	
	private void createCoClassModel() {
		coClassModel = new HashMap<>();
		
		Set<CCObject> generatedCoClassObjects = new HashSet<>();
		
		long limit = Math.round(W2V_MODEL_SIZE * COCLASS_MODEL_TERMS_FACTOR);
		
		// Create CoClass objects with terms from a subset of available terms
		Set<String> coClassTerms = objectTerms.stream().limit(limit).collect(Collectors.toSet());
		List<String> pooledTerms = coClassTerms.stream().collect(Collectors.toList());
		
		coClassTerms.forEach( term -> {
			/* A random amount of CoClass objects contain the same term. */
			Collections.shuffle(pooledTerms);
			List<String> sharedTerms = pooledTerms.subList(0, faker.number().numberBetween(1, COCLASS_MODEL_MAX_TERM_SHARE));
			sharedTerms.add(term);
				
			CCObject cc = new CCObject();
				
			sharedTerms.forEach( sharedTerm -> {
				cc.addNoun(sharedTerm);
			});
				
			cc.setIri(Integer.toString(cc.getNouns().hashCode()));
			
			assert(generatedCoClassObjects.add(cc));
		});
		
		// Generate CCObject that does not share a term with any other to verify that score is 1.0
		uniqueTerm = objectTerms.stream().skip(limit + 1).collect(Collectors.toList()).get(0);
		CCObject cc = new CCObject();
		cc.addNoun(uniqueTerm);
		assert(generatedCoClassObjects.add(cc));
	
		generatedCoClassObjects.forEach( ccObject -> {
			for (String noun : ccObject.getNouns()) {
				if (coClassModel.containsKey(noun)) {
					coClassModel.get(noun).add(ccObject);
				} else {
					List<CCObject> ccobjects = new ArrayList<>();
					ccobjects.add(ccObject);
					coClassModel.put(noun, ccobjects);
				}
			}
		});
	}
	
	public void printCoClassModel() {
		System.out.println("Terms: " + coClassModel.size());
		System.out.println("Objects: " + coClassModel.values().stream().collect(Collectors.toList()).size());
		IntSummaryStatistics stats = coClassModel.values().stream().collect(Collectors.summarizingInt(List::size));
		System.out.println("References per term (min/max/avg): " +  stats.getMin() + "/" + stats.getMax() + "/" + stats.getAverage());
		System.out.println("Total references: " + stats.getSum());
	}
	
	public void printW2vModel() {
		System.out.println(w2vModel.columnKeySet().stream().collect(Collectors.joining("\t", "\t\t", "")));
		w2vModel.rowKeySet().forEach( term -> {
			System.out.print(term + "\t");
			w2vModel.row(term).forEach( (key, value) -> {
				System.out.print(value + "\t\t");
			});
			System.out.print("\n");
		});
	}
}
