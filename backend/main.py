from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Optional
import databases
import sqlalchemy

# Database connection
DATABASE_URL = "postgresql://user:password@localhost/clue2crew"
database = databases.Database(DATABASE_URL)
metadata = sqlalchemy.MetaData()

families = sqlalchemy.Table(
    "families",
    metadata,
    sqlalchemy.Column("familyId", sqlalchemy.String, primary_key=True),
    sqlalchemy.Column("familyName", sqlalchemy.String),
    sqlalchemy.Column("pairingCode", sqlalchemy.String),
    sqlalchemy.Column("createdAt", sqlalchemy.BigInteger),
    sqlalchemy.Column("lastUpdated", sqlalchemy.BigInteger),
)

members = sqlalchemy.Table(
    "members",
    metadata,
    sqlalchemy.Column("memberId", sqlalchemy.String, primary_key=True),
    sqlalchemy.Column("familyId", sqlalchemy.String),
    sqlalchemy.Column("name", sqlalchemy.String),
    sqlalchemy.Column("deviceId", sqlalchemy.String),
    sqlalchemy.Column("status", sqlalchemy.String),
    sqlalchemy.Column("latitude", sqlalchemy.Float),
    sqlalchemy.Column("longitude", sqlalchemy.Float),
    sqlalchemy.Column("isMe", sqlalchemy.Boolean),
    sqlalchemy.Column("lastUpdated", sqlalchemy.BigInteger),
)

engine = sqlalchemy.create_engine(DATABASE_URL)
metadata.create_all(engine)

app = FastAPI(title="Clew2Crew Admin API")

class Family(BaseModel):
    familyId: str
    familyName: str
    pairingCode: str
    createdAt: int
    lastUpdated: int

class Member(BaseModel):
    memberId: str
    familyId: str
    name: str
    deviceId: str
    status: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    isMe: bool
    lastUpdated: int

@app.on_event("startup")
async def startup():
    await database.connect()

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()

@app.post("/api/families")
async def create_family(family: Family):
    query = families.insert().values(
        familyId=family.familyId,
        familyName=family.familyName,
        pairingCode=family.pairingCode,
        createdAt=family.createdAt,
        lastUpdated=family.lastUpdated
    )
    try:
        await database.execute(query)
        return {"status": "success"}
    except Exception as e:
        # Handle update if exists
        query = families.update().where(families.c.familyId == family.familyId).values(
            familyName=family.familyName,
            pairingCode=family.pairingCode,
            lastUpdated=family.lastUpdated
        )
        await database.execute(query)
        return {"status": "updated"}

@app.put("/api/families/{id}")
async def update_family(id: str, family: Family):
    query = families.update().where(families.c.familyId == id).values(
        familyName=family.familyName,
        pairingCode=family.pairingCode,
        lastUpdated=family.lastUpdated
    )
    await database.execute(query)
    return {"status": "success"}

@app.post("/api/members")
async def add_member(member: Member):
    query = members.insert().values(
        memberId=member.memberId,
        familyId=member.familyId,
        name=member.name,
        deviceId=member.deviceId,
        status=member.status,
        latitude=member.latitude,
        longitude=member.longitude,
        isMe=member.isMe,
        lastUpdated=member.lastUpdated
    )
    try:
        await database.execute(query)
        return {"status": "success"}
    except Exception:
        query = members.update().where(members.c.memberId == member.memberId).values(
            status=member.status,
            latitude=member.latitude,
            longitude=member.longitude,
            lastUpdated=member.lastUpdated
        )
        await database.execute(query)
        return {"status": "updated"}

@app.get("/api/admin/families", response_model=List[Family])
async def get_all_families():
    query = families.select()
    return await database.fetch_all(query)

@app.get("/api/admin/members", response_model=List[Member])
async def get_all_members():
    query = members.select()
    return await database.fetch_all(query)
