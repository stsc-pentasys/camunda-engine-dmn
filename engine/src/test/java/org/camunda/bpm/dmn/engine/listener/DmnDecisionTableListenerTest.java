/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.dmn.engine.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionTable;
import org.camunda.bpm.dmn.engine.DmnDecisionTableListener;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnDecisionTableRule;
import org.camunda.bpm.dmn.engine.DmnDecisionTableValue;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.dmn.engine.impl.DmnEngineConfigurationImpl;
import org.camunda.bpm.dmn.engine.test.DecisionResource;
import org.camunda.bpm.dmn.engine.test.DmnEngineRule;
import org.camunda.bpm.model.dmn.BuiltinAggregator;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DmnDecisionTableListenerTest {

  public static final String DMN_FILE = "org/camunda/bpm/dmn/engine/listener/DmnDecisionTableListenerTest.test.dmn";
  @Rule
  public DmnEngineRule dmnEngineRule = new DmnEngineRule(new DmnDecisionTableListenerEngineConfiguration());

  public DmnEngine engine;
  public DmnDecision decision;
  public TestDmnDecisionTableListener listener;


  @Before
  public void initEngineAndDecision() {
    engine = dmnEngineRule.getEngine();
    decision = dmnEngineRule.getDecision();
    listener = ((DmnDecisionTableListenerEngineConfiguration) engine.getConfiguration()).testDecisionTableListener;
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testListenerIsCalled() {
    Map<String, Object> variables = createVariables(true, "foo", "hello", "hello");
    engine.evaluate(decision, variables);
    assertThat(listener.result).isNotNull();
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testExecutedDecisionElements() {
    // the number should be independent from input and result
    evaluateDecision(true, "foo", false, "hello");
    assertThat(listener.result.getExecutedDecisionElements()).isEqualTo(36);

    evaluateDecision(false, "bar", true, "hello");
    assertThat(listener.result.getExecutedDecisionElements()).isEqualTo(36);

    evaluateDecision(false, "false", true, "hello");
    assertThat(listener.result.getExecutedDecisionElements()).isEqualTo(36);
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testInputValues() {
    evaluateDecision(true, "foo", "test", "hello");
    Map<String, DmnDecisionTableValue> inputs = listener.result.getInputs();
    assertThat(inputs).hasSize(3)
      .containsOnlyKeys("input1", "input2", "input3");

    DmnDecisionTableValue input1 = inputs.get("input1");
    assertThat(input1.getKey()).isEqualTo("input1");
    assertThat(input1.getName()).isEqualTo("Input");
    assertThat(input1.getOutputName()).isEqualTo("cellInput");
    assertThat(input1.getValue()).isEqualTo(true);

    DmnDecisionTableValue input2 = inputs.get("input2");
    assertThat(input2.getKey()).isEqualTo("input2");
    assertThat(input2.getName()).isNull();
    assertThat(input2.getOutputName()).isEqualTo("x");
    assertThat(input2.getValue()).isEqualTo("foo");

    DmnDecisionTableValue input3 = inputs.get("input3");
    assertThat(input3.getKey()).isEqualTo("input3");
    assertThat(input3.getName()).isNull();
    assertThat(input3.getOutputName()).isEqualTo("cellInput");
    assertThat(input3.getValue()).isNull();
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testMatchingRules() {
    evaluateDecision(true, "foo", "test", "hello");
    List<DmnDecisionTableRule> matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(1);

    DmnDecisionTableRule matchedRule = matchingRules.get(0);
    assertThat(matchedRule.getKey()).isEqualTo("rule1");
    assertThat(matchedRule.getOutputs()).hasSize(2);

    evaluateDecision(true, "bar", "test", "hello");
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(0);

    evaluateDecision(false, "bar", "test", "hello");
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(1);

    matchedRule = matchingRules.get(0);
    assertThat(matchedRule.getKey()).isEqualTo("rule2");
    assertThat(matchedRule.getOutputs()).hasSize(1);

    evaluateDecision(false, "bar", true, "hello");
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(5);

    matchedRule = matchingRules.get(0);
    assertThat(matchedRule.getKey()).isEqualTo("rule2");
    assertThat(matchedRule.getOutputs()).hasSize(1);
    matchedRule = matchingRules.get(1);
    assertThat(matchedRule.getKey()).isEqualTo("rule3");
    assertThat(matchedRule.getOutputs()).hasSize(0);
    matchedRule = matchingRules.get(2);
    assertThat(matchedRule.getKey()).isEqualTo("rule4");
    assertThat(matchedRule.getOutputs()).hasSize(1);
    matchedRule = matchingRules.get(3);
    assertThat(matchedRule.getKey()).isEqualTo("rule5");
    assertThat(matchedRule.getOutputs()).hasSize(1);
    matchedRule = matchingRules.get(4);
    assertThat(matchedRule.getKey()).isEqualTo("rule6");
    assertThat(matchedRule.getOutputs()).hasSize(1);
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testOutputs() {
    evaluateDecision(true, "foo", "test", "hello");
    List<DmnDecisionTableRule> matchingRules = listener.result.getMatchingRules();
    Map<String, DmnDecisionTableValue> outputs = matchingRules.get(0).getOutputs();
    assertThat(outputs).hasSize(2)
      .containsOnlyKeys("output1", "output2");

    DmnDecisionTableValue output1 = outputs.get("output1");
    assertThat(output1.getKey()).isEqualTo("output1");
    assertThat(output1.getName()).isEqualTo("Output 1");
    assertThat(output1.getOutputName()).isEqualTo("out1");
    assertThat(output1.getValue()).isEqualTo("hello");

    DmnDecisionTableValue output2 = outputs.get("output2");
    assertThat(output2.getKey()).isEqualTo("output2");
    assertThat(output2.getName()).isNull();
    assertThat(output2.getOutputName()).isNull();
    assertThat(output2.getValue()).isEqualTo("camunda");

    evaluateDecision(false, "bar", "test", "hello");
    matchingRules = listener.result.getMatchingRules();
    outputs = matchingRules.get(0).getOutputs();
    assertThat(outputs).hasSize(1)
      .containsOnlyKeys("output2");

    output2 = outputs.get("output2");
    assertThat(output2.getKey()).isEqualTo("output2");
    assertThat(output2.getName()).isNull();
    assertThat(output2.getOutputName()).isNull();
    assertThat(output2.getValue()).isEqualTo("camunda");
  }

  @Test
  @DecisionResource(resource = DMN_FILE)
  public void testCollectResult() {
    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) this.decision;
    decisionTable.setHitPolicy(HitPolicy.COLLECT);

    evaluateDecision(true, "bar", true, "hello");
    assertThat(listener.result.getCollectResultName()).isNull();
    assertThat(listener.result.getCollectResultValue()).isNull();
    List<DmnDecisionTableRule> matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(4);
    assertThat(matchingRules.get(0).getKey()).isEqualTo("rule3");
    assertThat(matchingRules.get(1).getKey()).isEqualTo("rule4");
    assertThat(matchingRules.get(2).getKey()).isEqualTo("rule5");
    assertThat(matchingRules.get(3).getKey()).isEqualTo("rule6");

    decisionTable.setAggregation(BuiltinAggregator.COUNT);
    evaluateDecision(true, "bar", true, "hello");
    assertThat(listener.result.getCollectResultName()).isEqualTo("collectMe");
    assertThat(listener.result.getCollectResultValue()).isEqualTo(3l);
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(4);
    assertThat(matchingRules.get(0).getKey()).isEqualTo("rule3");
    assertThat(matchingRules.get(1).getKey()).isEqualTo("rule4");
    assertThat(matchingRules.get(2).getKey()).isEqualTo("rule5");
    assertThat(matchingRules.get(3).getKey()).isEqualTo("rule6");

    decisionTable.setAggregation(BuiltinAggregator.SUM);
    evaluateDecision(true, "bar", true, "hello");
    assertThat(listener.result.getCollectResultName()).isEqualTo("collectMe");
    assertThat(listener.result.getCollectResultValue()).isEqualTo(90);
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(4);
    assertThat(matchingRules.get(0).getKey()).isEqualTo("rule3");
    assertThat(matchingRules.get(1).getKey()).isEqualTo("rule4");
    assertThat(matchingRules.get(2).getKey()).isEqualTo("rule5");
    assertThat(matchingRules.get(3).getKey()).isEqualTo("rule6");

    decisionTable.setAggregation(BuiltinAggregator.MAX);
    evaluateDecision(true, "bar", true, "hello");
    assertThat(listener.result.getCollectResultName()).isEqualTo("collectMe");
    assertThat(listener.result.getCollectResultValue()).isEqualTo(50);
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(4);
    assertThat(matchingRules.get(0).getKey()).isEqualTo("rule3");
    assertThat(matchingRules.get(1).getKey()).isEqualTo("rule4");
    assertThat(matchingRules.get(2).getKey()).isEqualTo("rule5");
    assertThat(matchingRules.get(3).getKey()).isEqualTo("rule6");

    decisionTable.setAggregation(BuiltinAggregator.MIN);
    evaluateDecision(true, "bar", true, "hello");
    assertThat(listener.result.getCollectResultName()).isEqualTo("collectMe");
    assertThat(listener.result.getCollectResultValue()).isEqualTo(10);
    matchingRules = listener.result.getMatchingRules();
    assertThat(matchingRules).hasSize(4);
    assertThat(matchingRules.get(0).getKey()).isEqualTo("rule3");
    assertThat(matchingRules.get(1).getKey()).isEqualTo("rule4");
    assertThat(matchingRules.get(2).getKey()).isEqualTo("rule5");
    assertThat(matchingRules.get(3).getKey()).isEqualTo("rule6");
  }

  // helper

  public DmnDecisionResult evaluateDecision(Object input1, Object input2, Object input3, Object output1) {
    Map<String, Object> variables = createVariables(input1, input2, input3, output1);
    return engine.evaluate(decision, variables);
  }

  public Map<String, Object> createVariables(Object input1, Object input2, Object input3, Object output1) {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("input1", input1);
    variables.put("input2", input2);
    variables.put("input3", input3);
    variables.put("output1", output1);
    return variables;
  }

  public static class DmnDecisionTableListenerEngineConfiguration extends DmnEngineConfigurationImpl {

    public TestDmnDecisionTableListener testDecisionTableListener = new TestDmnDecisionTableListener();

    public DmnDecisionTableListenerEngineConfiguration() {
      customPostDmnDecisionTableListeners.add(testDecisionTableListener);
    }

  }

  public static class TestDmnDecisionTableListener implements DmnDecisionTableListener {

    public DmnDecisionTableResult result;

    public void notify(DmnDecisionTable decisionTable, DmnDecisionTableResult decisionTableResult) {
      this.result = decisionTableResult;
    }

  }

}
