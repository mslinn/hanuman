Hanuman - Scala / Akka / BlueEyes / sbt demo
============================================

This project demonstrates the [BlueEyes](https://github.com/jdegoes/blueeyes) Scala framework,
Akka ````Actor````s and ````Ref````s; it can be run on any computer with Java installed, including
[Heroku](https://api.heroku.com/myapps/strong-galaxy-4334).

The ````BlueEyes```` framework encapsulates [Netty](http://netty.io/), which acts as a web container.
The ````Hanuman```` application services are defined by the ````BlueEyes```` DSL,
including services for ````HTML GET````, ````JSON GET```` and ````POST````, and MongoDB.
````HanumanService```` acts as a front-end for a hierarchy of Akka ````Actor````s, including ````Hanuman````
(the mythological monkey god), ````WorkVisor```` (an Akka ````supervisor````) and ````WorkCell````s
(which each contain a ````Monkey```` and a ````Critic````).

This application simulates the adage that a large number of monkeys typing long enough should eventually reproduce any
given document.
````Monkey```` instances generate pages of semi-random text, and their ````Critic````s compare the generated text to a
target document.
````WorkVisor```` actors supervise the ````WorkCell````s for a simulation.
Because ````HanumanService```` can support a multiplicity of simulaneous simulations,
a ````Hanuman```` actor supervises ````WorkVisors````.

The simulation is sequenced by ````tick````s.
````Monkey```` actors generate a page (by default, 1000 characters) of random text per ````tick````,
in the hope that they can match some portion of the target document.
To start a simulation, a client first requests a new simulation ID from ````HanumanService````.
_TODO provide the ability to upload the document that the ````Monkey````s are to attempt to replicate for a simulation._
Before generating random text, ````Monkey````s are first trained with a ````LetterProbabilities```` map of
````Char->probability```` when they are constructed.
A simulation terminates when a maximum number of ````tick````s have occurred, or the target document has been replicated.

````Monkey````s are unaware of the document they are attempting to replicate, and they are unaware of how the
````Critic```` works.
Likewise, ````Critic````s are unaware of how ````Monkey````s work.
One might imagine ever-more-sophisticated ````Monkey```` and ````Critic```` implementations.
For example, ````Monkey````s working from a dictionary should outperform ````Monkey````s that just type random characters.

````Critics```` send a ````TextMatch```` message to the ````WorkCell```` supervisor whenever a ````Monkey````'s
generated text has a better match than before to a passage in the target document.
Each simulation is managed by a ````WorkVisor```` actor/supervisor.
Hanuman stores the most recent ````TextMatch```` for the ````WorkCell```` in a ````TextMatchMap````,
which is defined as ````Map```` of ````Actor.Uuid -> TextMatch````.
Akka ````Ref````s are passed into each ````Hanuman```` and ````WorkCell```` actor/ supervisor, which sets/gets result
values atomically using shared-nothing state.

````Critic````s are computationally more expensive than ````Monkey````s are.
Because matches are statistally unlikely, and become exponentially less likely for longer sequences,
results per ````WorkCell```` are few.
This means that it is inexpensive to transmit results from a ````Critic```` in a ````WorkCell```` to its supervising
````WorkVisor```` via an Akka message.
Clients that might poll for results require a different strategy; a result cache is returned to them,
and the cache is updated on each ````tick```` by the ````Hanuman```` actor supervisor.

The ````HanumanService```` creates the ````Hanuman```` actor/supervisor, and the ````Hanuman```` constructor accepts an
Akka Ref to a ````Simulations``` instance, which is a ````Map```` of ````simulationID -> TextMatchMap````.
Getting values from the Ref and and setting new values are performed within an implicit software transaction, which is
computationally expensive and presents a possible choke point.
Marshalling changes via a sequence of ````ticks```` reduces potential conflicts.


Run Locally
-----------

1. Clone this [git repo](https://github.com/mslinn/hanuman).

2. Compile the app and create a start script:

        sbt stage

3. Run the app:

        sbt run


Run Clients Against Local or Remote Service Instances
-----------------------------------------------------

1. JSON service (without the correct `Content-Type header` there will be no response).

        curl --header "Content-Type:application/json" http://localhost:8585/json

2. The ````test```` script fully exercises the Hanuman web API. It can work against a local Hanuman service instance
or a remote service instance at a specified URL. Sample usages:

        ./test
        ./test http://hollow-winter-3011.herokuapp.com


Run on Heroku
-------------

Mike Slinn has deployed the app to http://hollow-winter-3011.herokuapp.com/

You can deploy it to your own Heroku app instance this way:

1. Clone the [git repo](https://github.com/mslinn/hanuman).

2. Install the [Heroku client](http://toolbelt.herokuapp.com/) and set up ssh keys.

3. Authenticate with Heroku:

        heroku login

4. Create your new app instance on Heroku:

        heroku create --stack cedar

5. Add your Heroku app instance as a remote git repository. Substitute your Heroku app instance for ````hollow-winter-3011````:

        git remote add heroku git@heroku.com:hollow-winter-3011.git

6. Push the Hanuman app to Heroku; it will automatically be (re)built and run.

        git push heroku master

You can also manually run the ````sbt```` console on Heroku:

    heroku run sbt console
