#!/usr/bin/env python3

import sys
import os
import json
import tempfile
import shutil
import subprocess
import argparse
import re

def judge_partial_output_to_valid_json(judge_output):
    valid_json = []

    while judge_output:
        parsed_json_object, amount_parsed_chars = json.JSONDecoder().raw_decode(judge_output)
        judge_output = judge_output[amount_parsed_chars:]
        
        if parsed_json_object["command"] == "append-message":
            parsed_json_object["message"]["description"] = re.sub("\n at [^\n]+\\([^)]+\\)", "", parsed_json_object["message"]["description"])

        valid_json.append(parsed_json_object)

    return valid_json

def run_exercise(path_to_exercise, abs_path_judge):
    assert os.path.exists(path_to_exercise) 
    assert os.path.isdir(path_to_exercise)
    assert os.path.exists(abs_path_judge)
    assert os.path.isfile(abs_path_judge)

    with open(os.path.join(path_to_exercise, "config.json")) as exercise_config:
        exercise_config_json = json.load(exercise_config)

        allow_compilation_warnings = False
        if "allow_compilation_warnings" in exercise_config_json:
            allow_compilation_warnings = exercise_config_json["allow_compilation_warnings"]

        filename = exercise_config_json["filename"]
        language = exercise_config_json["natural_language"]

        print(f"Testing \"{os.path.relpath(path_to_exercise)}\" with judge \"{os.path.relpath(abs_path_judge)}\" ... ")

        temp_workdir = tempfile.TemporaryDirectory()
        workdir = os.path.join(path_to_exercise, "workdir")
        if os.path.exists(workdir):
            assert os.path.isdir(workdir)
            shutil.copytree(workdir, temp_workdir.name, dirs_exist_ok=True)

        run_options = { 
            "resources": os.path.realpath(os.path.join(path_to_exercise, "evaluation")),
            "judge": os.path.dirname(abs_path_judge),
            "natural_language": language,
            "workdir": temp_workdir.name,
            "allow_compilation_warnings": allow_compilation_warnings,
            "filename": filename,
            "time_limit": 30,
            "memory_limit": 1000000000, 
            "source": os.path.join(path_to_exercise, "submission.java")
        }

        result = subprocess.run(abs_path_judge, input=json.dumps(run_options), cwd=temp_workdir.name, text=True, capture_output=True)
        cleaned_result = json.dumps(judge_partial_output_to_valid_json(result.stdout),
                                    indent = 4,
                                    sort_keys = True)

        with open(os.path.join(path_to_exercise, "result.json")) as expected_result_file:
            expected_result = expected_result_file.read().replace("\n", "")
            cleaned_expected_result = json.dumps(judge_partial_output_to_valid_json(expected_result),
                                                 indent = 4,
                                                 sort_keys = True)

            has_passed = (cleaned_result == cleaned_expected_result)

            if cleaned_result == cleaned_expected_result:
                print("PASSED")
                print()
            else:
                print("FAILED")
                print()

                print(">>> EXPECTED:")
                print(cleaned_expected_result)
                print()

                print(">>> ACTUAL:")
                print(cleaned_result)
                print()
            
            return has_passed


################################################################################

# Set up argparse

parser = argparse.ArgumentParser()
parser.add_argument(dest="path_judge", metavar="PATH_JUDGE", type=str, help="the path to the judge (\"e.g. ../run\")")
parser.add_argument("-d", dest="test_exercise_directory", metavar="TEST_EXCERCISE_DIRECTORY", type=str, help="the path to a test exercise directory (\"e.g. ./compile-error-student\")", required=False)
args = parser.parse_args()

# determine how to run this script

abs_path_judge = os.path.abspath(args.path_judge)
current_path = os.path.dirname(os.path.abspath(__file__))

assert os.path.exists(abs_path_judge) 
assert os.path.isfile(abs_path_judge)

print("Judge path:", abs_path_judge)
print("current path:", current_path) 
print()

if args.test_exercise_directory:
     run_exercise(os.path.abspath(args.test_exercise_directory), abs_path_judge)
else:

    passed = []
    failed = []

    for dir in os.listdir(path = current_path):
        path_to_exercise = os.path.join(current_path, dir)

        if (os.path.isdir(path_to_exercise)):
            has_exercise_passed = run_exercise(path_to_exercise, abs_path_judge)

            if has_exercise_passed:
                passed.append(os.path.basename(path_to_exercise))
            else:
                failed.append(os.path.basename(path_to_exercise))

    if passed:
        print("===== PASSED =====")
        for p in passed:
            print(p)

    print()
    
    if failed:
        print("===== FAILED =====")
        for f in failed:
            print(f)

     