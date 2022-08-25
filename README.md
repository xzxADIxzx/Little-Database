# Little Database
This database is designed to be and doesn't pretend to be *the best*, it's just good and little.

## Little Database Request
Abbreviated LDR. It is very similar to SQL queries but is designed to work with json.   
Each LDR starts with the name of the file to work with, then a cyclic construction from the action and parameters to it, and so on in a circle.

### Action and Parameters
| Action | Parameters | Description                            |
|--------|------------|----------------------------------------|
|get     |key         |Returns a value by a key in json.       |
|put     |key value   |Puts a value to json by a key.          |
|remove  |key         |Remove a value from json by a key.      |
|contains|key         |Returns whether the json contains a key.|
|each    |            |Iterate over all values in json.        |
|task    |key interval|Runs a task at an interval in seconds.  |
|stop    |key         |Stop a task by a key.                   |

### Each Action   
You can put *each* before an action to iterate over all values in the json.   
Example: `database get player-datas each get play-time add 1`   
This will increase the play-time in each player data.

### Tasks
The tasks run on the database server, so you don't have to send an LDR every *n* seconds.

To start a task, you need to send a regular LDR with a *task action* at the beginning.   
Example: `database task increase 60 put wave-time 0`   
This will set the wave-time to 0 every minute.

To stop a task, you need to send LDR with a *stop action* at the beginning.   
Example: `database stop increase`