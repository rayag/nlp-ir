import os
import json
import string
import unicodedata
import string
import re
import datetime
from media_dirs import *
from lemmagen3 import Lemmatizer

NEWS_BASE_DIR = '/home/lt38092/crawled_news'

class FileReader:
    """
    Class used for sequential file read from a given dir.
    This is useful because all texts could not be loaded
    into the memory before training
    """
    def __init__(self, base_dir = None):
        self.stop_words = self.read_stopwords() # a set of stop words
        self.media_dirs = MEDIA_DIR_MAP.values()
        self.base_dir = NEWS_BASE_DIR if base_dir is None else base_dir
        self.lem = Lemmatizer('bg')

    def read_stopwords(self):
        result = set()
        raw = open('./stop-words.txt', 'r').readlines()
        for word in raw:
            result.add(word.strip())
        return result

    def __iter__(self):
        for media_dir in self.media_dirs:
            media_abs_dir = self.base_dir + media_dir
            print(media_abs_dir)
            for fname in os.listdir(media_abs_dir):
                raw = open(os.path.join(media_abs_dir, fname), "r").read()
                parsed = json.loads(raw)
                body = [self.lem.lemmatize(s) for s in parsed["bodyTokens"]]
                title = [self.lem.lemmatize(s) for s in parsed["titleTokens"]]
                summary = [self.lem.lemmatize(s) for s in parsed["summaryTokens"]]
                yield [*title, *summary, *body]

    def iter_filename(self):
        for media_dir in self.media_dirs:
            media_abs_dir = self.base_dir + media_dir
            print(media_abs_dir)
            for fname in os.listdir(media_abs_dir):
                raw = open(os.path.join(media_abs_dir, fname), "r").read()
                parsed = json.loads(raw)
                body = parsed["bodyTokens"]
                title = parsed["titleTokens"]
                summary = parsed["summaryTokens"]
                yield [*title, *summary, *body], media_dir + fname
                
    def preprocess(self):
        print(f'Using base dir {self.base_dir}')
        for media_dir in self.media_dirs:
            media_abs_dir = self.base_dir + media_dir
            print(media_abs_dir)
            for fname in os.listdir(media_abs_dir):
                raw = open(os.path.join(media_abs_dir, fname), "r").read()
                parsed = json.loads(raw)
                parsed["bodyTokens"] = self.get_clear_token_list(parsed["bodyTokens"])
                parsed["titleTokens"] = self.get_clear_token_list(parsed["titleTokens"])
                parsed["summaryTokens"] = self.get_clear_token_list(parsed["summaryTokens"])
                with open(os.path.join(media_abs_dir, fname), "w", encoding='utf8') as f:
                    json.dump(parsed, f, ensure_ascii=False)
        

    def process_token(self, token):
        pattern = re.compile(r'[^а-яА-Я]')
        cleared = re.sub(pattern, '', token)
        cleared = self.lem.lemmatize(cleared)
        if cleared in self.stop_words or len(cleared) < 2:
            return None
        return cleared

    def preprocess_input(self, sentance):
        return self.get_clear_token_list(sentance.lower().split())            

    def get_clear_token_list(self, raw_tokens):
        result = list()
        for token in raw_tokens:
            token = token.split()
            for t in token:
                cleared = self.process_token(t)
                if cleared is not None:
                    result.append(cleared)
        return result

    def read_article(self, path):
        with open(self.base_dir + path, 'r') as f:
            json_obj = json.loads(f.read())
            return json_obj["titleTokens"], json_obj["summaryTokens"], json_obj["bodyTokens"]

    def get_property_from_json_file(self, abs_path, prop):
        with open(abs_path, 'r') as f:
            json_obj = json.loads(f.read())
            return json_obj[prop]

    def get_clean_article(self, path_processed):
        path_to_clean = re.sub(r"^(\/[a-z]*)-output(.*)$", r"\1\2", path_processed)
        with open(self.base_dir + path_to_clean, 'r') as f:
            json_obj = json.loads(f.read())
            return json_obj["title"], json_obj["summary"], json_obj["body"] 

    def get_by_year(self, year):
        for media_dir in self.media_dirs:
            media_abs_dir = self.base_dir + media_dir
            print(media_abs_dir)
            for fname in os.listdir(media_abs_dir):
                raw = open(os.path.join(media_abs_dir, fname), "r").read()
                parsed = json.loads(raw)
                if int(parsed["year"]) != year:
                    continue
                body = [self.lem.lemmatize(s) for s in parsed["bodyTokens"]]
                title = [self.lem.lemmatize(s) for s in parsed["titleTokens"]]
                summary = [self.lem.lemmatize(s) for s in parsed["summaryTokens"]]
                yield [*title, *summary, *body]
    
    def get_by_media_and_in_period(self, media, period_start, period_end):
        media_abs_dir = NEWS_BASE_DIR + MEDIA_DIR_MAP[media]
        for fname in os.listdir(media_abs_dir):
            raw = open(os.path.join(media_abs_dir, fname), "r").read()
            parsed = json.loads(raw)
            if int(parsed['month']) <= 0 or int(parsed['month']) > 12:
                continue
            article_date = datetime.datetime(int(parsed['year']), 
                int(parsed['month']), int(parsed['day']))
            if article_date > period_end or article_date < period_start:
                continue
            body = [self.lem.lemmatize(s) for s in parsed["bodyTokens"]]
            title = [self.lem.lemmatize(s) for s in parsed["titleTokens"]]
            summary = [self.lem.lemmatize(s) for s in parsed["summaryTokens"]]
            yield [*title, *summary, *body]
