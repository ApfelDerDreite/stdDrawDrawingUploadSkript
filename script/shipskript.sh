#!/bin/bash
#Clones all the repos
#1st arg is the file, 2nd arg is the name of the repos prefix eg. ship or helloworld
#
# Needs the following files present in the working dir: README.md(providid) StdDraw.class(you can use the provided version
# or the standart one. (Reader supports using the standart one, though it is less performant)
# Reader.class 
clonetodirname="cloned-repos"
clonedir="/tmp/$clonetodirname"
mkdir -p $clonedir
echo "Beginning Clone. Make sure that ssh-agent is running and has loaded your ssh key else this skript will hang"
echo "Please remove the entire dir /tmp/cloned-repos/ or implement automatic git pull. i cant be bothered to"
echo "Cloning repositories to $clonedir"
cat $1 | grep https |grep $2| cut -c 26- | awk '{print "git@github.com:"$1}' | parallel git -C "$clonedir" clone

num_repos=$(ls $clonedir | wc -l)
echo "$num_repos repos present after clone"
echo "Generate drawings? Note that running untrusted code without reading it might not be the best idea ever"
echo "(yes / else)"
read confirm

if [[ ${confirm,,} =~ "y" ]]; then
	if [[ -f "Reader.class" && -f "StdDraw.class" ]]; then
		echo "Found classes Reader.class and StdDraw.class"
		echo "Compiling classes..."
		ls $clonedir | parallel javac "/tmp/cloned-repos/{1}/Ship.java"
		if [[ $? -ne 0 ]]; then
			echo "Javac could not compile all repos"
		fi
		echo "Generating images..."
		paths=$(java Reader $clonedir/*/)
		if [[ ! -f "README.md" ]]; then
			echo "Error: Could not find file README.md."  "Please make sure that the provided file is in $(pwd)"
			exit 1
		fi
		echo "Updating README files"
		echo $paths
		if [[ "x$paths" = "x" ]]; then
			echo "No images were generated. Aborting"
			exit 1
		fi
		echo $paths | tr ' ' '\n' | parallel "cp ./README.md {1}README.md ; git -C {1} add README.md ; git -C {1} commit -m 'update README'">>/dev/null
		echo "Moving drawing to branch: badges..."
		branchname="badges"
		# TODO find a better way to do the split into lines. array perhaps?
		# TODO dont compile in the repo so we dont get merge conflicts with outdated .class files                                                                                                                                    just in case we update it
		echo $paths | tr ' ' '\n' | parallel "git -C {1} rm -f Ship.class && git -C {1} commit -m 'clean up .class file' ; git -C {1} add drawing.png ; git -C {1} status ; git -C {1} stash ; git -C {1} checkout $branchname ; git -C {1} rm drawing.png ; git -C {1} stash pop ; ls {1} ;git -C {1} stash clear ; git -C {1} commit -m 'added Drawing' ; git -C {1} checkout master">>/dev/null 2>>/dev/null
		echo "Finished moving files to branches"
		echo "Do you want to push the changes?"
		read confirm
		if [[ ! ${confirm,,} =~ "y" ]]; then
			echo "Terminating"
			exit 0
		fi
		echo "Pushing changes for all branches..."
		echo $paths | tr ' ' '\n' | parallel "git -C {1} push --branches"
		echo "Finished pushing changes" 
	else
		echo "Could not find the classes Reader.class and/or StdDraw.class. "
		echo "Please make sure they are present in $(pwd)"
		exit 1
	fi
fi


