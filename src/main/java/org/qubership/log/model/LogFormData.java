/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.log.model;

public class LogFormData {
    private String message;
    private Integer genTime;
    private Integer msgPerSec;
    private Integer numberOfRep;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getGenTime() {
        return genTime;
    }

    public void setGenTime(Integer genTime) {
        this.genTime = genTime;
    }

    public int getMsgPerSec() {
        return msgPerSec;
    }

    public void setMsgPerSec(Integer msgPerSec) {
        this.msgPerSec = msgPerSec;
    }

    public Integer getNumberOfRep() {
        return numberOfRep;
    }

    public void setNumberOfRep(Integer numberOfRep) {
        this.numberOfRep = numberOfRep;
    }
}
