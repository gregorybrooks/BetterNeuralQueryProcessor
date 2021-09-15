import json
import sys
import numpy as np
from sentence_transformers import SentenceTransformer

class EmbeddingsIndex():
    def __init__(self, target_corpus_dir, corpus_file_name, outname):
        self.target_corpus_dir = target_corpus_dir
        self.corpus_file_name = corpus_file_name
        self.outname = outname
#        self.model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')
        self.model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')


    def write_embedding(self, v):
        list_version = np.ndarray.tolist(np.asarray(v))
        json_version = json.dumps(list_version)
        return json_version

    def build_index(self):
        f = open(self.target_corpus_dir + "/" + self.corpus_file_name, "r")
        outfile = open(outname, "w")

        for x in f:
          l = json.loads(x)
          id=l['derived-metadata']['id']
          doctext=l['derived-metadata']['text']
          segments = l['derived-metadata']['segment-sections']
          sentences = []
          for sentence_descriptor in segments:
              sentences.append(doctext[sentence_descriptor['start']: sentence_descriptor['end']])
          embeddings = self.model.encode(sentences)
          outline = '{"id": "' + id + '", "emb": ' + self.write_embedding(embeddings) + '} \n'
          outfile.write(outline)

