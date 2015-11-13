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
 ******************************************************************************/
package com.ikanow.aleph2.harvest.logstash.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;

/** Listens in on a process's output and collects it into a string
 * @author Alex
 */
public class OutputCollectorService extends Thread {

	private long _MAX_CHARS = 1500000L; // (~3MB max length)
	private long _OVERFLOW_CHARS = 1000000L; // (~2MB max length)
	private InputStream _in;
	private PrintWriter _out;
	private StringBuffer _overflowBuffer = null;
	private int _lines = 0;
	private long _chars = 0;
	private boolean _pipelineStarted = false;
	public OutputCollectorService(InputStream in, PrintWriter out) {
		_in = in;
		_out = out;
	}		
	public int getLines() {
		return _lines;
	}
	public boolean getPipelineStarted() {
		return _pipelineStarted;
	}
	
	@Override
	public void run() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(_in));
			String line = null;
			while (null != (line = br.readLine())) {
				if (!_pipelineStarted) {
					if (line.contains(":message=>\"Pipeline started\"")) {
						_pipelineStarted = true;
					}
				}
				if (_chars < _MAX_CHARS) { // 8Mchars==16MB==max size of BSON object
					_out.println(line);
					_chars += line.length();
					if (_chars > _MAX_CHARS) {
						_out.println("[" + new Date() + "] WARNING: Logging truncated, > " + _MAX_CHARS + " characters");
						_overflowBuffer = new StringBuffer();
					}
				}//TESTED
				else if (null != _overflowBuffer){
					if (_overflowBuffer.length() >= _OVERFLOW_CHARS) {
						_overflowBuffer.replace(0, 1 + line.length(), ""); // (include the '\n')
					}
					_overflowBuffer.append(line).append('\n');
				}//TESTED
				_lines++;
				
				//DEBUG
				//System.out.println(line);
			}
		}
		catch (Exception e) {
			//DEBUG
			//e.printStackTrace();
		} 
		finally {
			try {
				if ((null != _out) && (null != _overflowBuffer)) {
					_out.println("...");
					_out.write(_overflowBuffer.toString());
				}
				if (null != br) br.close();
			}
			catch (IOException e) {
				//DEBUG
				//e.printStackTrace();
			}
		}
	}
}
