from django.urls import path

from . import views

urlpatterns = [
    path('', views.index, name='index'),
    path('searchresults/', views.submition, name='searchresults'),
    path('search/', views.index, name='search'),
    path('trackMe/', views.driverTracker, name='DriverTracker')
]