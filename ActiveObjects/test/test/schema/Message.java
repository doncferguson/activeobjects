/*
 * Copyright 2008 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.schema;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;

/**
 * @author Daniel Spiewak
 */
public interface Message extends Entity {
	
	@NotNull
	public String getContents();
	public void setContents(String contents);

	public Address getFrom();
	public void setFrom(Address from);

	public Address getTo();
	public void setTo(Address to);
}
