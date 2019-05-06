/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.metatron.discovery.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import app.metatron.discovery.config.security.WebSecurityDefaultConfiguration;
import app.metatron.discovery.config.security.WebSecuritySAMLConfiguration;

@Configuration
class WebSecurityConfiguration {

  @EnableWebSecurity
  @Configuration
  @ConditionalOnExpression("!${polaris.saml.enable:false}")
  // https://stackoverflow.com/questions/42822875/springboot-1-5-x-security-oauth2
  @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
  protected static class DefaultWebSecurityConfig extends WebSecurityDefaultConfiguration {

  }

  @EnableWebSecurity
  @Configuration
  @ConditionalOnExpression("${polaris.saml.enable:false}")
  // https://stackoverflow.com/questions/42822875/springboot-1-5-x-security-oauth2
  @Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
  protected static class SAMLWebSecurityConfig extends WebSecuritySAMLConfiguration {

  }
}

