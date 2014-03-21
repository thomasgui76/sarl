/*
 * Copyright 2014 Sebastian RODRIGUEZ, Nicolas GAUD, Stéphane GALLAND.
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
package io.sarl.lang.core;

import java.util.Map;



/** This interface represents a provider of built-in capacities.
 * The built-in capacities are assumed to be provided by
 * the runtime platform.
 * 
 * @author $Author: srodriguez$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public interface BuiltinCapacitiesProvider {
	
	/** Replies the built-in capacities for the given agent.
	 * 
	 * @param agent
	 * @return the built-in capacities for the given agent.
	 */
	public Map<Class<? extends Capacity>, Skill> getBuiltinCapacities(Agent agent);
	
}
