/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.asterix.app.data.gen;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.asterix.app.data.gen.RecordTupleGenerator.GenerationFunction;
import org.apache.asterix.om.types.ATypeTag;

public class ADoubleFieldValueGenerator implements IAsterixFieldValueGenerator<Double> {
    private static final double START = 1000000000.0;
    private static final int BATCH_SIZE = 1000;
    private static final double INCREMENT = 0.1;
    private final GenerationFunction generationFunction;
    private final boolean unique;
    private final boolean tagged;
    private final Random rand = new Random();
    private double value;
    private int cycle;
    private List<Double> uniques;
    private Iterator<Double> iterator;

    public ADoubleFieldValueGenerator(GenerationFunction generationFunction, boolean unique, boolean tagged) {
        this.generationFunction = generationFunction;
        this.unique = unique;
        this.tagged = tagged;
        switch (generationFunction) {
            case DECREASING:
                value = Integer.MAX_VALUE;
                break;
            case DETERMINISTIC:
                value = START;
                break;
            case INCREASING:
                value = 0;
                break;
            case RANDOM:
                if (unique) {
                    double lowerBound = START;
                    double upperBound = lowerBound + (BATCH_SIZE * INCREMENT);
                    uniques = new ArrayList<>();
                    while (lowerBound < upperBound) {
                        uniques.add(lowerBound);
                        lowerBound += INCREMENT;
                    }
                    Collections.shuffle(uniques);
                    iterator = uniques.iterator();
                }
            default:
                break;
        }
    }

    @Override
    public void next(DataOutput out) throws IOException {
        if (tagged) {
            out.writeByte(ATypeTag.SERIALIZED_DOUBLE_TYPE_TAG);
        }
        generate();
        out.writeDouble(value);
    }

    private void generate() {
        switch (generationFunction) {
            case DECREASING:
                value -= INCREMENT;
            case DETERMINISTIC:
                if (value >= START) {
                    cycle++;
                    value = START - (cycle * INCREMENT);
                } else {
                    value = START + (cycle * INCREMENT);
                }
                break;
            case INCREASING:
                value += INCREMENT;
                break;
            case RANDOM:
                if (unique) {
                    if (iterator.hasNext()) {
                        value = iterator.next();
                    } else {
                        // generate next patch
                        cycle++;
                        double lowerBound;
                        if (cycle % 2 == 0) {
                            // even
                            lowerBound = START + ((cycle / 2) * (BATCH_SIZE * INCREMENT));
                        } else {
                            // odd
                            lowerBound = START - ((cycle / 2 + 1) * (BATCH_SIZE * INCREMENT));
                        }
                        double upperBound = lowerBound + (BATCH_SIZE * INCREMENT);
                        uniques.clear();
                        while (lowerBound < upperBound) {
                            uniques.add(lowerBound);
                            lowerBound += INCREMENT;
                        }
                        Collections.shuffle(uniques);
                        iterator = uniques.iterator();
                        value = iterator.next();
                    }
                } else {
                    value = rand.nextDouble();
                }
                break;
            default:
                break;

        }
    }

    @Override
    public Double next() throws IOException {
        generate();
        return value;
    }

    @Override
    public void get(DataOutput out) throws IOException {
        if (tagged) {
            out.writeByte(ATypeTag.SERIALIZED_DOUBLE_TYPE_TAG);
        }
        out.writeDouble(value);
    }

    @Override
    public Double get() throws IOException {
        return value;
    }
}
