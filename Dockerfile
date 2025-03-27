# Copyright 2024-2025 NetCracker Technology Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM openjdk:18-slim-buster AS build

ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m" \
    LOG_MESSAGES_PER_SECOND=1000 \
    LOG_GENERATION_TIME=600 \
    LOG_MESSAGES_MULTILINE=false \
    LOG_MULTILINE_PROBABILITY=0.3 \
    LOG_TEMPLATES=java

COPY src src
COPY pom.xml pom.xml
COPY mvnw mvnw
COPY .mvn .mvn

RUN chmod +x ./mvnw \
    && ./mvnw clean package

FROM openjdk:18-slim-buster

RUN mkdir -p /opt/app/qubership-log-generator/etc
RUN mkdir -p /opt/app/static
RUN mkdir -p /opt/app/target

WORKDIR /opt/app

COPY --from=build /target/qubership-log-generator.jar ./target/

COPY charts/qubership-log-generator/config/ ./qubership-log-generator/etc/
COPY static/ ./static/

CMD ["java", "-jar", "/opt/app/target/qubership-log-generator.jar"]

EXPOSE 8080