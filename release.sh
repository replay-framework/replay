#!/usr/bin/env sh

./gradlew clean check publishToMavenLocal && ./gradlew publish --no-parallel --info