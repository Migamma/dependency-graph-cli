#!/usr/bin/env bash
set -uo pipefail

cd "$(dirname "$0")/.."

JAR="target/dependency-graph-cli-1.0-SNAPSHOT.jar"
CONFIGS="tests/configs"

CASES=(
    "valid.yaml|1|Cannot connect to repository"
    "valid-no-filter.yaml|1|Cannot connect to repository"
    "no-such-config.yaml|1|Cannot read file"
    "empty.yaml|1|must contain key-value pairs"
    "broken.yaml|1|Invalid YAML"
    "not-a-map.yaml|1|must contain key-value pairs"
    "duplicate-key.yaml|1|Invalid YAML"
    "unknown-key.yaml|1|Unknown parameter: maxDeph"
    "missing-package.yaml|1|Missing parameter: packageName"
    "empty-package.yaml|1|packageName must not be empty"
    "package-not-string.yaml|1|packageName must be a string"
    "bad-package-format.yaml|1|groupId:artifactId:version"
    "test-mode-bad-package.yaml|1|uppercase Latin letters"
    "missing-repository.yaml|1|Missing parameter: repository"
    "bad-repository-url.yaml|1|must be an http(s) URL"
    "test-mode-missing-file.yaml|1|Test repository file not found"
    "missing-output.yaml|1|Missing parameter: outputFile"
    "output-not-png.yaml|1|ending with .png"
    "filter-not-string.yaml|1|filter must be a string"
    "missing-test-mode.yaml|1|Missing parameter: testMode"
    "test-mode-not-boolean.yaml|1|testMode must be true or false"
    "missing-ascii-tree.yaml|1|Missing parameter: asciiTree"
    "ascii-tree-not-boolean.yaml|1|asciiTree must be true or false"
    "missing-depth.yaml|1|Missing parameter: maxDepth"
    "depth-not-integer.yaml|1|maxDepth must be an integer"
    "depth-fraction.yaml|1|maxDepth must be an integer"
    "depth-zero.yaml|1|maxDepth must be at least 1"
)

passed=0
failed=0

for case in "${CASES[@]}"; do
    IFS="|" read -r file expected_code expected_text <<< "$case"
    output=$(java -jar "$JAR" "$CONFIGS/$file" 2>&1)
    code=$?
    if [[ "$code" == "$expected_code" && "$output" == *"$expected_text"* ]]; then
        echo "PASS  $file"
        passed=$((passed + 1))
    else
        echo "FAIL  $file (code $code, output: $output)"
        failed=$((failed + 1))
    fi
done

echo
echo "Passed: $passed, failed: $failed"
[[ "$failed" == 0 ]]
