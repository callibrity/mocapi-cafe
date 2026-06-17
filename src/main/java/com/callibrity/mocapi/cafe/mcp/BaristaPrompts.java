/*
 * Copyright © 2026 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.callibrity.mocapi.cafe.mcp;

import com.callibrity.mocapi.api.prompts.McpPrompt;
import com.callibrity.mocapi.api.prompts.template.PromptTemplate;
import com.callibrity.mocapi.api.prompts.template.PromptTemplateFactory;
import com.callibrity.mocapi.model.GetPromptResult;
import com.callibrity.mocapi.model.Role;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PROMPTS — user-controlled templates. A person invokes these (often as a slash command); the
 * server returns ready-made messages for the model to start from.
 *
 * <p>The template is compiled once at construction via {@link PromptTemplateFactory} (Spring
 * {@code ${...}} placeholders) and rendered per call.
 */
@Component
public class BaristaPrompts {

  private final PromptTemplate recommendTemplate;

  public BaristaPrompts(PromptTemplateFactory factory) {
    this.recommendTemplate =
        factory.create(
            Role.USER,
            "Drink recommendation",
            "I'm in the mood for something ${mood}. Recommend one drink from the menu "
                + "(resource menu://drinks) and explain your pick in a sentence.");
  }

  @McpPrompt(
      name = "recommend-a-drink",
      description = "Ask the barista to recommend a drink for your mood")
  public GetPromptResult recommend(String mood) {
    return recommendTemplate.render(Map.of("mood", mood == null || mood.isBlank() ? "surprising" : mood));
  }
}
