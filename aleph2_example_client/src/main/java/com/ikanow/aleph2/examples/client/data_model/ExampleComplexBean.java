/*******************************************************************************
 * Copyright 2015, The IKANOW Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ikanow.aleph2.examples.client.data_model;

/** Example bean for accessing the CRUD service
 *  THINGS TO NOTE:
 *  1) No setters - use BeanTemplateUtils.clone() to mutate the object (this isn't a v2 requirement)
 *  2) The getter format is "scala style" not "java style"
 *  3) BeanTemplateUtils has loads of functions to let you build, clone, access fields without strings, convert to/from JSON
 * @author Alex
 */
public class ExampleComplexBean {

	public String _id() { return _id; }
	public String value() { return value; }
	public ExampleSubBean sub_object() { return sub_object; }
	
	private String _id;
	private String value;
	private ExampleSubBean sub_object;
}
