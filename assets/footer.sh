
if [ "${JERVIS_LANG}" = 'ruby' ]; then
  rvm gemset delete "${JERVIS_GEMSET}" --force
fi
