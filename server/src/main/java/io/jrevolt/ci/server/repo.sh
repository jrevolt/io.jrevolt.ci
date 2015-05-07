#!/bin/bash

set -eu


init() {
	[ -f .initialized ] || (
		repo="$1"
		git init --bare
		git remote add origin "$repo"
		touch .initialized
		echo "Repository initialized: $repo in $(realpath .)"
	)
}

onBranchCreated() {
	branch="$1"
   echo "Triggering TC build..."
   curl -s "$teamcity/httpAuth/action.html?add2Queue=${tcproject}&name=teamcity.build.branch&value=${branch}"
}

onMergeRequestCreated() {
	requestId="$1"
	srcrepo="$2"
	srcbranch="$3"
	dstbranch="$4"
	mrid="$5"
	mrbranch="$6"

	if git remote | grep -E "^${mrid}\$"; then
		echo "${mrid} : already created"
		exit
	fi

	echo "Merge request ${mrid} has been created. Publishing in master remote as merge/MR-$requestId..."

	echo "Registering remote..."
	git remote add -t "$srcbranch" --no-tags "${mrid}" "$srcrepo" || true

	echo "Fetching..."
	git fetch "${mrid}"

	echo "Publishing on master remote..."
	git push -f origin "refs/remotes/${mrid}/$srcbranch:refs/heads/${mrbranch}"

	echo "DONE: onMergeRequestCreated(${mrid})"
}

updateMergeRequestBranches() {
	git fetch --prune
}

onMergeRequestClosed() {
	requestId="$1"
	mrid="$2"
	mrbranch="$3"

	if ! git remote | grep -E "^${mrid}\$"; then
		echo "${mrid} : already closed"
		exit
	fi

	echo "Merge request ${mrid} has been closed. Cleaning up..."

	echo "Unregistering remote ${mrid}..."
	git remote remove "${mrid}" || true

	echo "Removing obsolete merge request branch on remote master..."
	git push origin ":${mrbranch}" || true

	echo "Pruning old references..."
	git gc --prune=now

	echo "DONE: onMergeRequestClosed(${mrid})"
}

$*