{-# LANGUAGE DataKinds #-}
{-# LANGUAGE DuplicateRecordFields #-}
{-# LANGUAGE OverloadedLists #-}
{-# LANGUAGE OverloadedStrings #-}

module Simplex.Chat.Operators.Presets where

import Data.List.NonEmpty (NonEmpty)
import qualified Data.List.NonEmpty as L
import Simplex.Chat.Operators
import Simplex.Messaging.Agent.Env.SQLite (ServerRoles (..), allRoles)
import Simplex.Messaging.Agent.Store.Entity
import Simplex.Messaging.Protocol (ProtocolType (..), SMPServer)

operatorSwitzerland :: NewServerOperator
operatorSwitzerland =
  ServerOperator
    { operatorId = DBNewEntity,
      operatorTag = Just OTSwitzerland,
      tradeName = "Switzerland Relay",
      legalName = Just "Arpokrat CH",
      serverDomains = ["amp1-ch.arpokrat.com", "xftp1-ch.arpokrat.com"],
      conditionsAcceptance = CARequired Nothing,
      enabled = True,
      smpRoles = allRoles,
      xftpRoles = allRoles
    }

operatorIceland :: NewServerOperator
operatorIceland =
  ServerOperator
    { operatorId = DBNewEntity,
      operatorTag = Just OTIceland,
      tradeName = "Iceland Relay",
      legalName = Just "Arpokrat IS",
      serverDomains = ["is.arpokrat.com"],
      conditionsAcceptance = CARequired Nothing,
      enabled = True,
      smpRoles = allRoles,
      xftpRoles = allRoles
    }

operatorPanama :: NewServerOperator
operatorPanama =
  ServerOperator
    { operatorId = DBNewEntity,
      operatorTag = Just OTPanama,
      tradeName = "Panama Relay",
      legalName = Just "Arpokrat PA",
      serverDomains = ["pa.arpokrat.com"],
      conditionsAcceptance = CARequired Nothing,
      enabled = True,
      smpRoles = allRoles,
      xftpRoles = allRoles
    }

operatorMalaysia :: NewServerOperator
operatorMalaysia =
  ServerOperator
    { operatorId = DBNewEntity,
      operatorTag = Just OTMalaysia,
      tradeName = "Malaysia Relay",
      legalName = Just "Arpokrat MY",
      serverDomains = ["my.arpokrat.com"],
      conditionsAcceptance = CARequired Nothing,
      enabled = True,
      smpRoles = allRoles,
      xftpRoles = allRoles
    }

operatorSouthAfrica :: NewServerOperator
operatorSouthAfrica =
  ServerOperator
    { operatorId = DBNewEntity,
      operatorTag = Just OTSouthAfrica,
      tradeName = "South Africa Relay",
      legalName = Just "Arpokrat ZA",
      serverDomains = ["za.arpokrat.com"],
      conditionsAcceptance = CARequired Nothing,
      enabled = True,
      smpRoles = allRoles,
      xftpRoles = allRoles
    }

allPresetServers :: NonEmpty SMPServer
allPresetServers = enabledSwitzerlandSMPServers

chSMPServers :: [NewUserServer 'PSMP]
chSMPServers =
  map (presetServer' True) (L.toList enabledSwitzerlandSMPServers)

enabledSwitzerlandSMPServers :: NonEmpty SMPServer
enabledSwitzerlandSMPServers =
  [ "smp://UBEAPOq_DAlDNn0I4svMwDHu2Y5FDNrfjOgTYOwvNSw=@amp1-ch.arpokrat.com,ampche4dmmpp7fcozptfwrj2c7mjzqzxldbio3udb6ihyiccjx7pdjid.onion"
  ]

isSMPServers, paSMPServers, mySMPServers, zaSMPServers :: [NewUserServer 'PSMP]
isSMPServers = []
paSMPServers = []
mySMPServers = []
zaSMPServers = []

{- Add removed SMP relay here
disabledSwitzerlandSMPServers :: NonEmpty SMPServer
disabledSwitzerlandSMPServers =
  [ "smp://removed@smp.arpokrat.com,removed.onion"
  ]
-}

chXFTPServers :: [NewUserServer 'PXFTP]
chXFTPServers =
  map
    (presetServer True)
    [ "xftp://1_d1Xuhp8DfnYl3DVSDTKpkVH7xijkeElO6Lpoesmdo=@xftp1-ch.arpokrat.com,xftpch3mjgaf4hpvv6cufefx5oi5rm7nc2cdm5nb47sfbrw4gqazqpqd.onion"
    ]

isXFTPServers, paXFTPServers, myXFTPServers, zaXFTPServers :: [NewUserServer 'PXFTP]
isXFTPServers = []
paXFTPServers = []
myXFTPServers = []
zaXFTPServers = []