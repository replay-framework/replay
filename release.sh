#!/usr/bin/env sh

./gradlew clean check install && ./gradlew uploadArchives --no-parallel --info