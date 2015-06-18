# piton

[![Build Status](https://travis-ci.org/ThomasMeier/piton.svg?branch=master)](https://travis-ci.org/ThomasMeier/piton)

A Clojure library for performing SQL migrations and seeding.

## Installation

Piton has two parts: a Leiningen plugin and a project library.
The plugin is intended to be installed as part of your personal lein profile.
The library must be included in your project to perform migrations and seeding
in both local and production environments.

In your `~/.lein/profiles.clj`:

    {:user {:plugins [[lein-piton "0.1.0"]]}}

In each `project.clj` that will use piton,

    {:dependencies [[piton "0.1.0"]]}

Then, in your **project's** `profiles.clj`, you'll need to give `lein-piton`
this information.

    {:dev {:piton {:dburl "jdbc:postgresql://localhost:5432/yourdb"
                   :dbuser "postgres"
                   :dbpass "secret"}}}

Optionally, you can also add `:mig-path` and `:seed-path` to the piton map
 above to specify different directories within your resources directory to
  place migration files

## Usage

#### Development environment

During development, you'll be using the Leiningen plugin to assist in creating
and storing migration files. By default, you'll find all of these in
`resources`.

    $ lein piton new mig add-table-to-my-database

To create a new seed

    $ lein piton new seed insert-data-to-my-table

In both of the above cases, a file will be generated for you. Inside each is a
necessary comment stating: `-- rollback`, write the migration above it, and its
rollback underneath.

To perform migrations and seeds

    # Apply all
    $ lein piton migrate
    $ lein piton seed

    # Apply selectively
    $ lein migrate add-table-to-my-database
    $ lein seed insert-data-to-my-table

To perform rollbacks

    $ lein piton rollback seeds
    $ lein piton rollback migrations
    $ lein piton rollback migrations add-table-to-my-database
    $ lein piton rollback seeds insert-data-to-my-table

You can also handle all migrations programmatically if you need to customize
 things further.

#### Production environment

When you create an uberjar and deploy, you will need to run the piton core class
with arguments for password and username and database address.

    $ java -cp your-standalone.jar piton.live $dburl $dbuser $dbpass [migrate|seed|rollback]

## License

Copyright © 2015 Thomas Meier

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
