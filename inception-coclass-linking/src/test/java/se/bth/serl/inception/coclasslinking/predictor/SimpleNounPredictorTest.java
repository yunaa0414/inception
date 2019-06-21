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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import se.bth.serl.inception.coclasslinking.recommender.Term;
import se.bth.serl.inception.coclasslinking.util.TestFixture;

public class SimpleNounPredictorTest {
	private static TestFixture fix;
	private static IPredictor predictor;
	
	@Mock
	private RecommenderContext recommenderContext;
	
	@Rule public MockitoRule mockitoRule = MockitoJUnit.rule(); 

	@BeforeClass
	public static void setup() {
		fix = new TestFixture();
		predictor = new SimpleNounPredictor(fix.getCoClassModel());
	}
	
	@Test
	public void testScoreOfUnknownTerm() {
		Term term = fix.generateUnknownTerm();
		
		Map<String, Double> result = predictor.score(recommenderContext, term);
		assertEquals(0, result.size());
	}
	
	@Test
	public void testScoreOfEmptyTerm() {
		Term term = new Term("");
		
		Map<String, Double> result = predictor.score(recommenderContext, term);
		assertEquals(0, result.size());
	}
	
	@Test
	public void testScoreOfKnownTerm() {
		Term term = fix.getRandomKnownTerm();
		
		Map<String, Double> result = predictor.score(recommenderContext, term);
		assertTrue(result.size() > 0);
		result.forEach( (k,v) -> {
			assertTrue("score (" + v + ") should be larger than 0.0", v > 0.0);
			assertTrue("score (" + v + ") should be smaller or equal to 1.0", v <= 1.0);
		});
	}
	
	@Test
	public void testScoreOfUniqueTerm() {
		Term term = fix.getUniqueTerm();
		
		Map<String, Double> result = predictor.score(recommenderContext, term);
		assertEquals(1, result.size());
		result.forEach( (k,v) -> {
			assertEquals(1.0, v.doubleValue(), 0.0);
		});
	}
}
