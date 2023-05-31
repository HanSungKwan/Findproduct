import sys
import os
import torch
import torch.distributed as dist
import torch.multiprocessing as mp

def main():
     args = parse_args(sys.argv[1:])
     state = load_checkpoint(args.checkpoint_path)
     initialize(state)

     # torch.distributed.run ensures that this will work
     # by exporting all the env vars needed to initialize the process group
     torch.distributed.init_process_group(backend=args.backend)

     for i in range(state.epoch, state.total_num_epochs):
          for batch in iter(state.dataset):
              train(batch, state.model)

          state.epoch += 1
          save_checkpoint(state)