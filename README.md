# bobtailbot

This is a simple little chatbot written in Clojure, mostly to have fun and learn about Clojure and also chatbots, AI, you name it. It can either talk through the command-line or connect to an irc server. It doesn't do much in terms of talking for now, but the idea is to make it accept [Clara](http://www.clara-rules.org/) rules and facts in some DSL or controlled natural language.
For now, it only takes rules and responds to queries from a toy [example](https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/shopping.clj).

I picked Clojure as a language because:

* It's a LISP, so it should be well-suited for all things AI.
* It runs on the JVM, so I get access to plenty of NLP tools and Semantic Web tools, which I'd like to use.

The original version was based on [Gigasquid's speech acts classifier](https://github.com/gigasquid/speech-acts-classifier), but I've removed the NLP stuff for now and added a few more things, like:
* irc connectivity from [AdamBoxall's irc client](https://github.com/AdamBoxall/clojure-irc-client), perhaps also with some code from [Nakkaya](https://nakkaya.com/2010/02/10/a-simple-clojure-irc-client/).
* Event publishing and subscribing from an official [example](https://github.com/clojure/core.async/wiki/Pub-Sub).
* The DSL from the already mentioned Clara [example](https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/shopping.clj).

If you need one of those production-ready chatbots written in Clojure, maybe have a look at these:

* [Yetibot](https://github.com/devth/yetibot)
* [Jubot](https://github.com/liquidz/jubot)
* [Clojurebot](https://github.com/hiredman/clojurebot)

## USAGE

By default, Bobtailbot connects to an irc server running locally, with host 127.0.0.1 and port 6667, and then it join a default irc channel, which is #whateverhey. These options can be changed in `config.clj`.
So, to get started quickly (for Ubuntu and similar):

## 1. Set up a local chat server, connect to it and join the default channel #whateverhey

 1. Install an irc server like [inspircd](http://www.inspircd.org/) (`sudo apt-get install inspircd`) and start it with the defaults. Check the configuration file with `sudo nano /etc/inspircd/inspircd.conf`. You shouldn't need to edit it.
 2. To start the server, do `sudo service inspircd start`. I took the instructions from [this blog post](https://samuelhewitt.com/blog/2016-04-09-how-to-deploy-an-irc-server-on-ubuntu).
 3. Start your favorite irc client, for instance [Hexchat](https://hexchat.github.io/) (if you don't have it installed, first do `sudo apt-get install hexchat`).
 4. In the Hexchat menu, do [Hexchat]->[Network list]->[Add], call this network "Localnet" (or whatever, it's not important).
 5. Edit Localnet so that in the Servers tab you've got `127.0.0.1/6667` and in the "connection commands" tab you've got `JOIN #whateverhey`.
 6. Connect to Localnet with a nick of your choice.

## 2. Download and run Bobtailbot, so that it connects to this chat server and joins the channel.
 1. Clone this repo: `git clone https://github.com/martinodb/bobtailbot`
 2. `cd ./bobtailbot`
 3. `lein run`, or `lein repl` and then `(-main)`.
