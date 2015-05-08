/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.utils.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class scriptRemoveInstancesWithMissing{


    public static void removeInstancesWithMissing(String path)  throws IOException {
        ARFFDataReader reader= new ARFFDataReader();
        reader.loadFromFile(path);

        String newPath = path.replace(".arff", "_NOMissing.arff");

        FileWriter fw = new FileWriter(newPath);
        fw.write("@relation dataset\n\n");

        for (Attribute att : reader.getAttributes()){
            fw.write(ARFFDataWriter.attributeToARFFString(att)+"\n");
        }

        fw.write("\n\n@data\n\n");

        DataStreamFromFile data = new DataStreamFromFile(reader);

        data.stream().forEach(e -> {
            boolean missing = false;
            for (Attribute att : reader.getAttributes()) {
                if(Utils.isMissingValue(e.getValue(att))){
                    missing = true;
                    break;
                }
            }
            try {
                if(!missing)
                fw.write(ARFFDataWriter.dataInstanceToARFFString(reader.getAttributes(), e) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();

    }
    public static void main(String[] args) {
        try {
            //removeInstancesWithMissing("/Users/ana/Documents/core/datasets/dynamicDataOnlyContinuous.arff");
            removeInstancesWithMissing(args[0]);
        }catch (IOException ex){}
    }
}