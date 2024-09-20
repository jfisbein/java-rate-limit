#!/bin/bash -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

export VERSION=$1

if [[ -z "${VERSION}" ]]; then
  echo "ERROR: Version is undefined"
  exit 1
fi

#git pull
./mvnw clean test --batch-mode

./mvnw versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
git add pom.xml
git commit -m "Set version ${VERSION}"
git push
git tag "${VERSION}"
git push --tags
