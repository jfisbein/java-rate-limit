#!/bin/bash -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

export VERSION=$1

if [[ -z "${VERSION}" ]]; then
  echo "ERROR: Version is undefined"
  exit 1
fi

#git pull
./mvnw versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
./mvnw clean test --batch-mode
git checkout .
git tag "${VERSION}"
git push --tags
