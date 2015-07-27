if [ "${GROOVY_VERSION}" = "1.8.9" ]; then
  echo "Submitting results to coveralls."
  ./gradlew cobertura coveralls
fi
