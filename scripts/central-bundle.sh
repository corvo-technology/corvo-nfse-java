#!/usr/bin/env bash
# Builds the Maven Central bundle (target/central-bundle.zip) from the signed artifacts that
# `mvn -Prelease verify` leaves in target/. The layout is the plain Maven repository layout with
# an .asc signature and md5/sha1 checksums for every file, and nothing else (the Central publishing
# plugin also staged maven-metadata-local.xml and _remote.repositories, which Central rejects).
set -euo pipefail

GROUP_PATH="tech/corvo"
ARTIFACT="corvo-nfse"
VERSION="$(mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout)"
DIR="target/central-bundle/${GROUP_PATH}/${ARTIFACT}/${VERSION}"

rm -rf target/central-bundle target/central-bundle.zip
mkdir -p "$DIR"

cp pom.xml "${DIR}/${ARTIFACT}-${VERSION}.pom"
for suffix in "" "-sources" "-javadoc"; do
  cp "target/${ARTIFACT}-${VERSION}${suffix}.jar" "${DIR}/"
done

# Detached signatures made by the maven-gpg-plugin (jar, sources, javadoc and pom).
cp target/${ARTIFACT}-${VERSION}*.asc "${DIR}/"

for f in "${DIR}"/*; do
  openssl dgst -md5 -r "$f" | cut -d' ' -f1 | tr -d '\n' > "${f}.md5"
  openssl dgst -sha1 -r "$f" | cut -d' ' -f1 | tr -d '\n' > "${f}.sha1"
done

(cd target/central-bundle && zip -qr ../central-bundle.zip .)
echo "Bundle:"; unzip -l target/central-bundle.zip | awk '{print $4}' | grep -v '^$' | sed '1,2d'
