# B1. Modular Components of EDEN

EDEN is designed with a number of major components in mind which fulfil 
functions in their respective domain.
There are currently four major modules as described in the [original EDEN paper.](https://www.cn.hhu.de/nc/publikationen.html?publication=MeterSchneider2018a)  

* DGEP
* Interface
* Database
* Aggregator

The coming sections further detail the tasks and general thought behind the modules.

## DGEP
DGEP stands for Dialog-Game Execution Platform. This is the part that accepts an argument from the user as input and answers with one or multiple arguments in turn, chosen by some internal logic. 
The DGEP can also have more functions, like conduct reviews of arguments. All functions of the DGEP have in common that they use arguments as their resource.  

The default DGEP currently used by EDEN is [D-BAS](https://github.com/hhucn/dbas). D-BAS was developed to conduct online discussions using natural language, while providing structured data in the background. 

## Interface
The Interface currently communicates with the DGEP via an API to receive information about the next steps regarding user input. It also communicates with the aggregator to receive arguments.  

The default Interface currently used is [discuss](https://github.com/hhucn/discuss), which is a Clojurescript app, that can be used on any website.


## Database
The database mainly stores the arguments in a fashion that they can be easily searched. We currently use elasticsearch as a default store, which gives us the benefit of having easily accessible semantic search.

## Aggregator
The aggregator is the communication central of EDEN. The aggregator orchestrates the argument flow between the local and foreign EDEN instances. Furthermore the aggregator also manages internal dataflow to ensure that only well-formed and updated arguments are exchanged. The aggregator utilizes RabbitMQ queues and a RESTful API to disseminate arguments.


## Replacing Modules
If you want to replace any of the modules with a custom version, you can consult ["Replacing Modules"](c01_replacing_modules.html) for that. 
