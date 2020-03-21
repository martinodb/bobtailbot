Bobtailbot - A simple chatbot written in Clojure, with the ability to use different brains and adapters.
======

# _About_

This is a simple little chatbot written in Clojure, mostly to have fun and learn about Clojure and also chatbots, AI, you name it. It can either talk through the command-line or connect to an irc server.
For the moment, with its default brain, it only accepts simple facts described in SVO sentences with proper names, and simple general rules and queries, as depicted in the example interaction below.

I picked Clojure as a language because:

* It's a LISP, so it should be well-suited for all things AI.
* It runs on the JVM, so I get access to plenty of NLP tools and Semantic Web tools, which I'd like to use.

# _Usage_

By default, Bobtailbot connects to an irc server running locally, with host 127.0.0.1 and port 6667, and then it join a default irc channel, which is #bobtailbot. These options can be changed in `config.clj`.
So, to get started quickly (for Ubuntu and similar):

## _1._ Set up a local chat server, connect to it and join the default channel #bobtailbot

  1. Install an irc server like [inspircd](http://www.inspircd.org/) (`sudo apt-get install inspircd`) and start it with the defaults. Check the configuration file with `sudo nano /etc/inspircd/inspircd.conf`. You shouldn't need to edit it.
  2. To start the server, do `sudo service inspircd start`. I took the instructions from [this blog post](https://samuelhewitt.com/blog/2016-04-09-how-to-deploy-an-irc-server-on-ubuntu).
  3. Start your favorite irc client, for instance [Hexchat](https://hexchat.github.io/) (if you don't have it installed, first do `sudo apt-get install hexchat`).
  4. In the Hexchat menu, do [Hexchat]->[Network list]->[Add], call this network "Localnet" (or whatever, it's not important).
  5. Edit Localnet so that in the Servers tab you've got `127.0.0.1/6667` and in the "connection commands" tab you've got `JOIN #bobtailbot`.
  6. Connect to Localnet with a nick of your choice.

## _2._ Download and run Bobtailbot, so that it connects to this chat server and joins the channel.
  1. Clone this repo: `git clone https://github.com/martinodb/bobtailbot`
  2. `cd ./bobtailbot`
  3. Launch it with one of these commands:
     * `lein run`
     * `lein repl` and then `(-main)`.
     * `lein repl` and then `(dev-main  "-a irc" "-b general")`
  
  The last method (using `dev-main`) is very useful for development. It's just like `main` but when it's done it returns to the lein repl instead of closing the program. You can talk to the bot directly in the repl instead of irc, without changing the configuration file. Just change the command to:
  
   `(dev-main  "-a repl" "-b general")`

   If, in addition, you want to try out, say, the brain "template" instead of "general", do:

   `(dev-main  "-a repl" "-b template")`



## _3._ Edit the [configuration file](config.edn) to explore other options. Uncomment and replace as needed, as described in the documentation of [the outspace.config library](https://github.com/outpace/config).
  - By default, there will be a number of commented entries such as `#_bobtailbot.core/greeting #_"Hello.  Let's chat."`.
  - To make it run as a repl instead of connecting to irc, replace the line `#_bobtailbot.core/user-interface #_:irc` with `bobtailbot.core/user-interface :repl #_:irc`. Notice that the default value `:irc` is left commented. You can remove it if you prefer.
  - To connect to Freenode instead of Localnet, replace the line `#_bobtailbot.core/host #_"127.0.0.1"` with `bobtailbot.core/host "chat.freenode.net"`.
  - If the config file gets garbled and you need to regenerate it with default values, do `lein config`.

If you are using `lein repl`, then after editing `config.edn` or any other file,
   do `(user/refresh)`, and then again `(-main)`. Sometimes, if you edit namespace names and things like that, it's better to do `quit` and `lein repl` again, though.

## _4._ Register your bot name in Freenode and configure your bot so that it can login as user rather than only as guest.
 
 Follow the [nickname registration instructions. ](https://freenode.net/kb/answer/registration) You will need a valid email to make a separate account for your bot, which should be different from the one you use as a regular user (assuming you are a registered Freenode user). So let's say your bot's name is `mybot` and and your password is `mypasswd` , then in the Bobtailbot configuration you shold set as `nick` the string `"mybot\\0mybot\\0mypasswd"`. If at some point you want to login as guest, use another nick, like `mybot-guest`.
 
 You will want to connect to Freenode as a regular user to talk to your bot. Configure Hexchat so that you connect to `chat.freenode.net/+6697` , that is, using port `6697` rather than `6667`, so that you are not using the same port for your bot  and your regular user, which may cause disconnection problems (the bot should always use port `6667`). Traffic through port `6697` is encrypted using SSL, while traffic through `6667` is plainly visible, using, for instance, the command:
    ` sudo tcpflow -i any -C -g port 6667`.

 For instance, to pass all these options through the CLI, it would be:
     `lein run -a irc -b general -H chat.freenode.net -p 6667 -n mybot\\0mybot\\0mypasswd -c mychannel` 
 You can instead use `lein repl` and then:
     `(dev-main "-a irc" "-b general" "-H chat.freenode.net" "-p 6667" "-n mybot\\0mybot\\0mypasswd" "-c mychannel" )`
 Where `mychannel` will connect the bot to the IRC channel `#mychannel`.

 Of course, only test your bot in a channel made by you or one where you have permission.

# _Example interactions_

## _Example interaction with default brain: facts, rules, queries_

![Example interaction with default brain: facts, rules, queries](https://raw.githubusercontent.com/martinodb/bobtailbot/master/doc/screencap-2019-02-06%2013-00-32-v2.png "Example interaction with default brain: facts, rules, queries")


## _Example interaction with default brain: introducing vocabulary_

![Example interaction with default brain: introducing vocabulary](https://raw.githubusercontent.com/martinodb/bobtailbot/master/doc/screepcap-2019-02-09%2023-38-34.png "Example interaction with default brain: introducing vocabulary")
*You can edit this example:`{:add-voc-type :verb , :content {:inf "admire",:past "admired", :pp "admired",:er "admirer", :ing "admiring", :pres3 "admires"}}`*

## _Other brains_

The development status of other brains is very rudimentary at this point.

The "template" brain is a minimalistic example, and it should stay that way.

The "zinc" brain is, right now, basically a repl that uses the Zinc library as a backend. [Zinc](https://github.com/martinodb/Zinc) is just a trivial fork I made of [CSNePS](https://github.com/SNePS/CSNePS) that can be used as a library (CSNePS is only available standalone, AFAIK). Zinc is now a bit outdated compared to upstream CSNePS. You can look at [this gist](https://gist.github.com/martinodb/0a2982705d5df9993abc910b3e7a1597) for a small example of basic interaction. For more information see the [zinc brain source file](https://github.com/martinodb/bobtailbot/blob/master/src/bobtailbot/brains/zinc/brain.clj) and CSNePS documentation.

# _Development_

To write a new brain, you can start by looking at the [available brains](https://github.com/martinodb/bobtailbot/tree/master/src/bobtailbot/brains) , pick one, save it with a new name, then rename its namespaces accordingly.
Then you have to [add its namespace](https://github.com/martinodb/bobtailbot/blob/master/src/bobtailbot/core.clj) as a requirement in `bobtailbot.core` (this step is currently not necessary because brains are loaded dynamically, but it doesn't hurt, so you can do it if you run into issues).

Every brain must have a `brain.clj` file, so that the full namespace is `bobtailbot.brains.<brain-name>.brain`. It must also have the functions `respond`, `hear` and `speakup`. The first one is used for syncronous, strictly interactive UIs such as a repl, while the other two are used for asyncronous UI such as IRC, where the bot must be able to hear without saying anything, and OTOH it may speak up at any moment, for instance when it detects some kind of event outside of the chat environment.
Brain data are stored by convention in the directory `./data/bobtailbot/brains/<brain-name>/store`. In the example brains there's a snippet that detects the namespace and gives you the string `data-dir-prefix` to prepend to your storage file names. Just copy-paste it and leave it unmodified, or use one of those brains as a template.


To write a new adapter, look at the [available adapters](https://github.com/martinodb/bobtailbot/tree/master/src/bobtailbot/adapters) , pick one, save it with a new name, etc (same thing as with brains). Every adapter must have a function `(connect [{:keys [nick host port group-or-chan greeting hear speakup ]}])`.
You'll get the idea of how it works by looking at [bobtailbot.core](https://github.com/martinodb/bobtailbot/blob/master/src/bobtailbot/core.clj).

And that's it!

Bobtailbot is not intended to be a rich modular chatbot framework/platform you spend time learning so that you can write small modular functions that add to its behavior in combination with other small modules. Instead, it's intended to be thin glue between your chatbot brain, written in plain vanilla Clojure, and UI services like IRC, so you can test your ideas.


# _Credit_

Here I mention open-source projects, blogs, etc, that I found useful and sometimes took code from (attribution given in the source files when applicable).

* [Gigasquid's speech acts classifier](https://github.com/gigasquid/speech-acts-classifier).
* [AdamBoxall's irc client](https://github.com/AdamBoxall/clojure-irc-client), perhaps also with some code from [Nakkaya](https://nakkaya.com/2010/02/10/a-simple-clojure-irc-client/).
* Event publishing and subscribing from an official [example](https://github.com/clojure/core.async/wiki/Pub-Sub).
* The DSL from the already mentioned Clara [example](https://github.com/cerner/clara-examples/blob/master/src/main/clojure/clara/examples/shopping.clj).

Also, if you need a production-ready chatbot written in Clojure, maybe have a look at these:

* [Yetibot](https://github.com/devth/yetibot)
* [Jubot](https://github.com/liquidz/jubot)
* [Clojurebot](https://github.com/hiredman/clojurebot)
