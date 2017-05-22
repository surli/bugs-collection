/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.webaudio;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

public interface OscillatorNode extends AudioNode {
    String TYPE_SINE = "sine";
    String TYPE_SQUARE = "square";
    String TYPE_SAWTOOTH = "sawtooth";
    String TYPE_TRIANGLE = "triangle";
    String TYPE_CUSTOM = "custom";

    @JSProperty
    void setType(String type);

    @JSProperty
    String getType();

    @JSProperty
    AudioParam getFrequency();

    @JSProperty
    AudioParam getDetune();

    @JSProperty("onended")
    void setOnEnded(EventListener<MediaEvent> listener);

    @JSProperty("onended")
    EventListener<MediaEvent> getOnEnded();

    void start(double when);

    void start();

    void stop(double when);

    void stop();

    void setPeriodicWave(PeriodicWave periodicWave);
}
