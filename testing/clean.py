#!/usr/bin/env python3

import os
import sys

extensions = [
    ".java",
    ".class",
    ".jar"
]

for item in os.listdir():
    for extension in extensions:
        if item.endswith(extension):
            os.remove(item)