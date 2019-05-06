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

package app.metatron.discovery.query.druid.datasource;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by kyungtaak on 2017. 5. 22..
 */
@JsonTypeName("union")
public class UnionDataSource implements DataSource {

  public static UnionDataSource of(Iterable<String> names) {
    return new UnionDataSource(
        StreamSupport.stream(names.spliterator(), false)
            .map(name -> new TableDataSource(name))
            .collect(Collectors.toList())
    );
  }

  private List<TableDataSource> dataSources;


  public UnionDataSource(List<TableDataSource> dataSources) {
    this.dataSources = dataSources;
  }

  public List<TableDataSource> getDataSources() {
    return dataSources;
  }

  public void setDataSources(List<TableDataSource> dataSources) {
    this.dataSources = dataSources;
  }
}
