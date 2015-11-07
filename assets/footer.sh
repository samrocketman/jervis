
if [ "${JERVIS_LANG}" = 'ruby' ]; then
  rvm gemset delete "${JERVIS_GEMSET}" --force
fi

if [ "${JERVIS_LANG}" = 'go' ]; then
  cd -
  rm -rf "${GOPATH}"
fi
