package com.oxywatt.diagnostic.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class CollectorDatabase(context: Context, name: String) :
    SQLiteOpenHelper(context.applicationContext, name, null, 2) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE batches (id TEXT PRIMARY KEY NOT NULL, digest TEXT NOT NULL, revision INTEGER NOT NULL UNIQUE, epoch TEXT NOT NULL, document TEXT NOT NULL)"
        )
        db.execSQL("CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, snapshot TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE observations (position INTEGER PRIMARY KEY, epoch TEXT NOT NULL, sequence INTEGER NOT NULL, batch TEXT NOT NULL REFERENCES batches(id), session TEXT REFERENCES sessions(id), segment INTEGER, data TEXT NOT NULL, UNIQUE(epoch,sequence))"
        )
        db.execSQL("CREATE INDEX observations_session ON observations(session,position)")
        db.execSQL(
            "CREATE TABLE effects (batch TEXT NOT NULL REFERENCES batches(id), item INTEGER NOT NULL, data TEXT NOT NULL, PRIMARY KEY(batch,item))"
        )
        db.execSQL(
            "CREATE TABLE collector_state (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL, checkpoint TEXT NOT NULL)"
        )
        createResults(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion == 1 && newVersion == 2) { "Unsupported collector database migration" }
        createResults(db)
    }

    private fun createResults(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE analysis_runs (generation INTEGER PRIMARY KEY AUTOINCREMENT, request TEXT NOT NULL UNIQUE, session TEXT NOT NULL REFERENCES sessions(id), metric TEXT NOT NULL, cohort TEXT NOT NULL, provenance TEXT NOT NULL, terminal TEXT)"
        )
        db.execSQL(
            "CREATE TABLE analysis_slots (session TEXT NOT NULL REFERENCES sessions(id), metric TEXT NOT NULL, cohort TEXT NOT NULL, newest INTEGER NOT NULL REFERENCES analysis_runs(generation), PRIMARY KEY(session,metric,cohort))"
        )
        db.execSQL(
            "CREATE TABLE aggregate_members (aggregate_id TEXT NOT NULL, generation INTEGER NOT NULL REFERENCES analysis_runs(generation), PRIMARY KEY(aggregate_id,generation))"
        )
    }
}
