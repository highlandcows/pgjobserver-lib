#!/bin/sh -l
if [ -d $GITHUB_WORKSPACE ]
then
  sbt test
else
  bash
fi