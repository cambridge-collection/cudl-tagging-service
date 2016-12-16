# HTTP API

The tagging service provides an HTTP API to manipulate a user's tag data.

## Authentication

The default configuration uses JSON Web Tokens to authenticate API users.
Unauthenticated requests receive a 401 Unauthorized response with a
`WWW-Authentiate` header containing a `Bearer` challenge.

Requests which are to be considered authenticated must contain a token. This is
sent in the `Authorization` header with a value of the form `Bearer $TOKEN`.

## Endpoints

### `GET` `/crowdsourcing/anno/{docId}/{docPage}`

Get the annotations created by the authenticated user on the specified document
page.

```shell-session
$ curl -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/anno/MS-ADD-03430/1 \
    | python -m json.tool
{
    "annotations": [
        {
            "date": "2016-12-10 12:53:01 UTC",
            "name": "Bob",
            "page": 1,
            "position": {
                "coordinates": [
                    {
                        "x": 3246.395061728395,
                        "y": 6027.969135802551
                    }
                ],
                "type": "point"
            },
            "raw": 1,
            "target": "tag",
            "type": "person",
            "uuid": "98575bf6-f3ee-4a72-9074-831441a5191d",
            "value": 1.0
        },
        {
            "date": "2016-12-12 12:17:26 UTC",
            "name": "Pies",
            "page": 1,
            "raw": 1,
            "target": "doc",
            "type": "about",
            "uuid": "ed684bff-37f9-42f2-97fe-2ca431679e06",
            "value": 1.0
        },
        {
            "date": "2016-12-13 13:35:35 UTC",
            "name": "550-1450",
            "page": 1,
            "position": {
                "coordinates": [
                    {
                        "x": 4504.526234567901,
                        "y": 3318.1481481482315
                    }
                ],
                "type": "point"
            },
            "raw": 1,
            "target": "tag",
            "type": "date",
            "uuid": "ee80a5b1-55e5-494e-8389-66c10a578d40",
            "value": 1.0
        }
    ],
    "docId": "MS-ADD-03430",
    "oid": "raven:feb6ca5fe134c9e48f9b692ba184780d962b00bc05200729fa3f7e9071504a5a"
}
```

### `POST` `/crowdsourcing/anno/{docId}`

Create a new annotation on a document. Response is `201` and the body is the
created annotation.

```shell-session
$ echo '{"page":1,"target":"tag","type":"about","name":"Something","raw":1,"position":{"type":"point","coordinates":[{"x":1877.5321100917436,"y":3191.8149847095638}]}}' \
    | curl --data-binary @- -H 'Content-Type: application/json' \
        -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/anno/MS-ADD-03430 \
    | python -m json.tool
{
    "date": "2016-12-15 11:08:55 UTC",
    "name": "Something",
    "page": 1,
    "position": {
        "coordinates": [
            {
                "x": 1877.5321100917436,
                "y": 3191.8149847095638
            }
        ],
        "type": "point"
    },
    "raw": 1,
    "target": "tag",
    "type": "about",
    "uuid": "075f5625-e72a-4707-91e6-7400816e57e7",
    "value": 1.0
}
```

### `DELETE` `/crowdsourcing/anno/{docId}/{uuid}`

Delete a single annotation from a document. Response status is `204` on a
successful deletion, or `404` if no annotation with the UUID existed.

```shell-session
$ curl -X DELETE -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/anno/MS-ADD-03430/7dc08fdb-b95f-4e05-9f7e-33f93f0f243a
```

### `DELETE` `/crowdsourcing/anno/{docId}`

Delete multiple annotations in a document. The request body should be form data
containing one or more `uuid` parameters specifying the UUIDs to delete.

The response is a JSON array containing the UUIDs that were deleted.

```shell-session
$ curl -X DELETE -sSH "Authorization: Bearer $JWT" \
        -F uuid=075f5625-e72a-4707-91e6-7400816e57e7 \
        -F uuid=5e150223-66b3-4a29-9386-160f0e620af9 \
        -F uuid=a-a-a-a-a \
        http://tagging.example.com/crowdsourcing/anno/MS-ADD-03430 \
    | python -m json.tool
[
    "075f5625-e72a-4707-91e6-7400816e57e7",
    "5e150223-66b3-4a29-9386-160f0e620af9"
]
```

### `GET` `/crowdsourcing/tag/{docId}`

Get aggregated, ranked tags related to a document.

```shell-session
$ curl -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/tag/MS-ADD-03430 \
    | python -m json.tool
{
    "docId": "MS-ADD-03430",
    "oid": null,
    "terms": [
        {
            "name": "zawj",
            "raw": 1,
            "value": 0.08195
        },
        {
            "name": "half",
            "raw": 6,
            "value": 0.4918
        },

        [...]

        {
            "name": "ha-levi",
            "raw": 6,
            "value": 0.4918
        },
        {
            "name": "informal",
            "raw": 5,
            "value": 0.40985
        }
    ]
}
```

### `GET` `/crowdsourcing/rmvtag/{docId}`

Get the tags that a user has marked as inaccurate/unhelpful etc.

```shell-session
$ curl -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/rmvtag/MS-ADD-03430 \
    | python -m json.tool
{
    "docId": "MS-ADD-03430",
    "oid": "raven:feb6ca5fe134c9e48f9b692ba184780d962b00bc05200729fa3f7e9071504a5a",
    "tags": [
        {
            "name": "nisan",
            "raw": -1,
            "value": -1.0
        },
        {
            "name": "foobar",
            "raw": -1,
            "value": -1.0
        },
        {
            "name": "foobar2",
            "raw": -1,
            "value": -1.0
        },
        {
            "name": "foobar3",
            "raw": -1,
            "value": -1.0
        },
        {
            "name": "foobar4",
            "raw": -1,
            "value": -1.0
        }
    ]
}
```

### `POST` `/crowdsourcing/rmvtag/{docId}`

Mark a tag as being inaccurate/unhelpful.

```shell-session
$ echo '{"name": "foo", "raw": -1}' \
    | curl --data-binary @- -H 'Content-Type: application/json' \
        -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/rmvtag/MS-ADD-03430 \
    | python -m json.tool
{
    "docId": "MS-ADD-03430",
    "oid": "raven:feb6ca5fe134c9e48f9b692ba184780d962b00bc05200729fa3f7e9071504a5a",
    "tags": [
        {
            "name": "nisan",
            "raw": -3,
            "value": 0.0
        },
        {
            "name": "foobar",
            "raw": -1,
            "value": 0.0
        },

        [...]

        {
            "name": "foo",
            "raw": -1,
            "value": -1.0
        }
    ]
}
```

### `DELETE` `/crowdsourcing/rmvtag/{docId}/{tag}`

Unmark a tag as being inaccurate/unhelpful. Response is `204` for a successful
deletion, or `404` if the tag didn't exist.

```shell-session
$ curl -v -X DELETE -sH "Authorization: Bearer $JWT" \
        http://tagging.example.com/crowdsourcing/rmvtag/MS-ADD-03430/foo
```
