import json
import copy
import sys
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity


def get_score(v):
    s = 0.0
    for example_doc_sentence in v:
        for candidate_sentence_score in example_doc_sentence:
            if candidate_sentence_score > 0.5:
                s += candidate_sentence_score
    return s

#model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')

dir = sys.argv[1]   # TARGET CORPUS DIR

index2 = {}
with open(dir + '/embeddings.index') as json_file:
    for line in json_file:
        json_line = json.loads(line)
        id = json_line['id']
        embedding_as_list = json_line['emb']
        x = np.asarray(embedding_as_list)
        index2[id] = x

ranking = {}
lines = []
while True:
    for line in sys.stdin:
        line = line.strip()
        if line == 'EOD':
            example = model.encode(lines)
            for key, value in index2.items():
                candidate_similarity_scores = cosine_similarity(example, value)
                candidate_score = get_score(candidate_similarity_scores)
                if candidate_score > 0.0:
                    ranking[key] = candidate_score
            cnt = 1
            for key, value in sorted(ranking.items(),
                                     key=lambda item: item[1],
                                     reverse=True):
                print(key, value)
                cnt += 1
                if cnt > 1000:
                    break
            print('EOL')
            sys.stdout.flush()
            lines.clear()
            ranking.clear()
        elif line.startswith('EOF'):
            sys.exit(0)
        else:
            lines.append(line)


