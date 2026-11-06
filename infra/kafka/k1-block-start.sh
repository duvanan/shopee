#!/bin/sh
# K1 is config-only. K2 must replace this gate in a reviewed change.
# Deliberately ignore all arguments/environment; never start or format Kafka.
printf '%s\n' 'K1_ACTIVATION_BLOCKED: complete reviewed K2 TLS/mTLS and ACL provisioning before starting Kafka.' >&2
exit 78
